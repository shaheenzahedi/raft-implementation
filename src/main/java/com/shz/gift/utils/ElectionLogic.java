package com.shz.gift.utils;

import java.util.Random;

import com.shz.gift.preps.IMember;
import com.shz.gift.preps.Algo;
import com.shz.gift.protocol.RequestForVote;
import com.shz.gift.protocol.VoteGranted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionLogic {
	
	public static long DEFAULT_LEADER_TIMEOUT = 10000L;
	
	public static long DEFAULT_ELECTION_TIMEOUT = 10000L;
	
	public static long PING_LOOP = 1000L;
	
	public static long DEFAULT_STALE_MEMBER_TIMEOUT = DEFAULT_LEADER_TIMEOUT;
	
	private final Random random = new Random();
	
	private final Logger logger = LoggerFactory.getLogger(Algo.class);
	
	private Election election;

	protected long votedForTerm = -1;

	private long leaderTimestamp = System.currentTimeMillis();
	
	private final long leaderTimeout = DEFAULT_LEADER_TIMEOUT;
	
    private long electionTime = -1L;
    
    private long electionEnd = -1L;

    private final Algo algo;
    
    public ElectionLogic(Algo algo) {
		super();
		this.algo = algo;
	}


	public void voteReceived(VoteGranted vote) {
		logger.info("vote received: " + vote + " " + algo);
		if (algo.getRole() != Role.CANDIDATE) {
			return;
		}
		if (vote.getTerm() == algo.getTerm().getCurrent()) {
			election.incrementVotes();
			if (election.hasWon()) {
				election = null;
				algo.changeRole(Role.LEADER);
				logger.info("Becoming leader: " + algo + " in term=" + algo.getTerm().getCurrent());
				System.out.println("Becoming leader: " + algo + " in term=" + algo.getTerm().getCurrent());
				initLeader();
			}
		} else {
			logger.info("Vote received for old election: " + vote);
		}
	}
	

	public void setElectionTime(long electionTime) {
		logger.info("Setting new electiontime: " + electionTime + " " + algo);
		this.electionTime = electionTime;
	}


	public void setFollower() {
		setLeaderTimestamp(System.currentTimeMillis());
		electionEnd = -1L;
		setElectionTime(-1L);
		election = null;
		algo.changeRole(Role.FOLLOWER);
	}


	private void initLeader() {
		for (IMember m : algo.getMembers()) {
			m.setNextIndex(algo.getILog().getLastIndex() + 1);
			m.setMatchIndex(0L);
			m.setLastCommandReceived(System.currentTimeMillis());//new leader didn't receive any command, don't fill backlog write away
		}
		algo.leaderLoop();//starts sending log
	}


	public long followLoop() {
		if (electionTime > 0L) {
			return tryStartElection();
		}
		long time = System.currentTimeMillis();
		if (leaderTimeout < (time - leaderTimestamp)) {
			algo.changeRole(Role.CANDIDATE);
			electionEnd = -1L;
			long rdiff = random.nextInt((int)DEFAULT_ELECTION_TIMEOUT) + 1;
			setElectionTime(time + rdiff);
			return electionTime - time;
		}
		return -1L;
	}
	private long tryStartElection() {
		long time = System.currentTimeMillis();
		if (electionTime <= time) {
			setElectionTime(-1L);
			this.algo.getTerm().newTerm();
			this.election = new Election(algo.getMembers().size() + 1, this.algo.getTerm().getCurrent());
			for (IMember m : algo.getMembers()) {
				m.getChannel().send(algo, new RequestForVote(this.algo.getTerm().getCurrent(), algo.getILog().getLastIndex(), algo.getILog().getLastTerm()));
			}
			if (vote(null, new RequestForVote(this.algo.getTerm().getCurrent(), algo.getILog().getLastIndex(), algo.getILog().getLastTerm()))) {
				voteReceived(new VoteGranted(this.algo.getTerm().getCurrent()));
			}
			this.electionEnd = time + DEFAULT_ELECTION_TIMEOUT;
			logger.info("Starting election for: " + this.algo.getTerm().getCurrent());
			return DEFAULT_ELECTION_TIMEOUT;
		} else {
			return electionTime - time;
		}
	}

	public long candidateLoop() {
		long time = System.currentTimeMillis();
		if (electionEnd > -1 && electionEnd <= time) {
			//timeout
			setFollower();
		} else {
			if (electionEnd < 0) {
				return tryStartElection();				
			}
		}
		return -1L;
	}

	public boolean vote(IMember IMember, RequestForVote vote) {
		long currentTerm = algo.getTerm().getCurrent();
		if (vote.getTerm() > currentTerm) {
			algo.getTerm().setCurrent(vote.getTerm());
			if (algo.getRole() != Role.FOLLOWER) {
				setFollower();
			}
		}
		if (vote.getTerm() < currentTerm) {
			logger.info("vote.getTerm() < raft.getTerm().getCurrent(): "  + algo);
			return false;
		}
		if (votedForTerm >= vote.getTerm()) {
			logger.info("votedForTerm >= vote.getTerm(): "  + algo);
			return false;
		}
		if (vote.getLastLogTerm() < algo.getILog().getLastTerm()) {
			logger.info("vote.getLastLogTerm() < raft.getLog().getLastTerm(): "  + algo);
			return false;
		}
		if (vote.getLastLogIndex() < algo.getILog().getLastIndex()) {
			logger.info("vote.getLastLogIndex() < raft.getLog().getLastIndex(): "  + algo);
			return false;
		}
		this.votedForTerm = vote.getTerm();
		logger.info("setting voted for: " + this.votedForTerm + ": " + algo);
		return true;
	}

	

	public void setLeaderTimestamp(long time) {
		this.leaderTimestamp = time;
		
	}

}
