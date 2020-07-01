package com.shz.gift.preps;

import java.util.ArrayList;
import java.util.List;

import com.shz.gift.handler.ICommitHandler;
import com.shz.gift.log.ILog;
import com.shz.gift.log.LogImpl;
import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.AppendResponse;
import com.shz.gift.protocol.IMsg;
import com.shz.gift.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algo {

	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<IMember> members = new ArrayList<>();

	private Role role = Role.FOLLOWER;
	
	private final Term term = new Term();

	private final ILog ILog = new LogImpl(term);
	
	private final ElectionLogic election = new ElectionLogic(this);

	private String id;
	
    public void setMembers(List<? extends IMember> m) {
    	this.members.clear();
    	this.members.addAll(m);
    }
    
	
	/**
	 * Maintenance loop. Returns the time the loop should be executed again
	 * @return timeout for next run or -1L for default timeout
	 */
	public long loop() {
		switch (role) {
		case CANDIDATE:
			return getElection().candidateLoop();
		case FOLLOWER:
			return getElection().followLoop();
		case LEADER:
			return leaderLoop();
		}
		return -1L;
	}

	public long leaderLoop() {
		for (IMember m : members) {
			if (m.getMatchIndex() < ILog.getLastIndex() && System.currentTimeMillis() - m.getLastCommandReceived() > ElectionLogic.DEFAULT_STALE_MEMBER_TIMEOUT) {
				logger.info("Filling backlog for: " + m + " " + m.getNextIndex() + " " + ILog.getLastIndex());
				m.setNextIndex(ILog.getLastIndex());
				m.getChannel().send(this, copy(ILog.get(m.getNextIndex())));
				m.setLastCommandReceived(System.currentTimeMillis());//start backlog process only once per timeout
			} else {
				//ping
				if (ILog.getLastIndex() < 0) {
					m.getChannel().send(this, new AppendRequest(0L, 0L, term.getCurrent(), 0L, 0L, 0L, null));
				} else {
					AppendRequest ar  = ILog.get(ILog.getLastIndex());
					m.getChannel().send(this, 
							new AppendRequest(ar.getLogTerm(), ar.getIndex(), term.getCurrent(),
									ar.getPreviousIndex(),
									ar.getPreviousTerm(), ILog
											.getLastCommitIndex(), null));
					
				}
			}
		}
		return -1L;
	}


	private AppendRequest copy (AppendRequest ar) {
		return new AppendRequest(ar.getLogTerm(), ar.getIndex(), term.getCurrent(),
				ar.getPreviousIndex(), ar.getPreviousTerm(), ILog.getLastCommitIndex(), ar.getPayload());
	}
	
	public void changeRole(Role role) {
		logger.info("New role: " + role);
		this.role = role;
	}
	
	public boolean append(AppendRequest entry) {
		logger.info("append: " + entry);				
		if (role == Role.FOLLOWER) {
			this.getElection().setLeaderTimestamp(System.currentTimeMillis());
			return ILog.append(entry);
		} else if (role == Role.LEADER && entry.getLeaderTerm() > term.getCurrent()) {
			getElection().setFollower();
		} else if (role == Role.CANDIDATE && entry.getLeaderTerm() >= term.getCurrent()) {
			getElection().setFollower();
		}
		if (term.getCurrent() < entry.getLeaderTerm()) {
			term.setCurrent(entry.getLeaderTerm());
		}
		return false;
	}
	

	public Term getTerm() {
		return term;
	}

	public void handleClientRequest(IMsg channel, Object event) {
		if (role == Role.LEADER) {
			AppendRequest req = new AppendRequest(term.getCurrent(),
					ILog.getLastIndex()+1,
					term.getCurrent(), 
					ILog.getLastIndex(),
					ILog.getLastTerm(),
					ILog.getLastCommitIndex(),
					event, 
					channel);
			
			if (!ILog.append(req)) {
				//problem with statemachine
				throw new IllegalStateException("Log didn't accept new entry: " + req + ": " + ILog);
			}
			for (IMember m : members) {
				if (m.getNextIndex() == req.getIndex()) {
					m.setNextIndex(m.getNextIndex()+1);//increment optimistically to minimize retries
					m.getChannel().send(this, req);
				}
			}
		} else {
			throw new NotLeaderException();
		}
	}

	public void handleResponse(IMember member, AppendResponse event) {
		if (role != Role.LEADER) {
			return;
		}
		if (term.getCurrent() < event.getCurrentTerm()) {
			term.setCurrent(event.getCurrentTerm());
			election.setFollower();
			return;
		}
		
		member.setLastCommandReceived(System.currentTimeMillis());
		
		if (event.isSuccess()) {
			if (member.getNextIndex() <= event.getEntryIndex()) {
				member.setNextIndex(event.getEntryIndex() + 1);	
			}
			if (member.getMatchIndex() < event.getEntryIndex()) {
				member.setMatchIndex(event.getEntryIndex());
				
				if (event.getEntryTerm() == term.getCurrent()) {
					if (ILog.getLastCommitIndex() < event.getEntryIndex())  {
						long newCommit = getNextCommitIndex(ILog.getLastCommitIndex());
						if (newCommit > ILog.getLastCommitIndex()) {
							ILog.commit(newCommit);
						}
					}
				} 
			}
			if (member.getNextIndex() <= ILog.getLastIndex()) {
				member.getChannel().send(this, copy(ILog.get(member.getNextIndex())));
			}
		} else {
			member.setNextIndex(member.getNextIndex() - 1);
			if (member.getNextIndex() < 0L) {
				member.setNextIndex(0L);
			}
			if (member.getNextIndex() <= ILog.getLastIndex()) {
				member.getChannel().send(this, copy(ILog.get(member.getNextIndex())));
			}
		}
	}

	private long getNextCommitIndex(long currentAgreed) {
		int votesForNext = 0;
		votesForNext++;//my vote
		for (IMember m : members) {
			if (m.getMatchIndex() > currentAgreed) {
				votesForNext++;
			}
		}
		if ((members.size() + 1)/2 < votesForNext) {
			return getNextCommitIndex(currentAgreed+1);
		}
		return currentAgreed;
	}

	
	
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public void setCommitHandler(ICommitHandler cm) {
		ILog.setCommitHandler(cm);
	}

	public Role getRole() {
		return role;
	}

	public ILog getILog() {
		return ILog;
	}

	public List<IMember> getMembers() {
		return members;
	}


	public ElectionLogic getElection() {
		return election;
	}


	@Override
	public String toString() {
		return "Raft [" + (role != null ? "role=" + role + ", " : "")
				+ (id != null ? "id=" + id : "") + "]";
	}

	
}
