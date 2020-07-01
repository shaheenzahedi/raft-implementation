package com.shz.gift.preps;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.shz.gift.utils.ElectionLogic;
import com.shz.gift.utils.NotLeaderException;
import com.shz.gift.handler.IHandler;
import com.shz.gift.handler.QueueTask;
import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.AppendResponse;
import com.shz.gift.protocol.ClientRequest;
import com.shz.gift.protocol.ClientResponse;
import com.shz.gift.protocol.IMsg;
import com.shz.gift.protocol.RaftEvent;
import com.shz.gift.protocol.RequestForVote;
import com.shz.gift.protocol.VoteGranted;
import com.shz.gift.protocol.RaftEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventQueue implements IListener, IHandler<RaftEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Algo algo;

	private final ScheduledExecutorService executor;

	private final QueueTask<RaftEvent> queue = new QueueTask<>(this);

	private final LoopEvent loopEvent = new LoopEvent();
	
	private boolean running = true;

	public EventQueue(Algo algo, ScheduledExecutorService executor) {
		super();
		this.algo = algo;
		this.executor = executor;
	}

	public void setMembers(List<IMember> remoteMembers) {
		algo.setMembers(remoteMembers);
	}

	public void init() {
		running = true;
		loop(1L);
	}

	private void loop(long l) {
		if (running) {
			if (loopEvent.scheduleCounter.get() < 1) {
				loopEvent.scheduleCounter.incrementAndGet();
				executor.schedule(loopEvent, l, TimeUnit.MILLISECONDS);				
			} else {
				logger.error("did not schedule loop for: " + algo);
			}
		}
	}

	@Override
	public void handleEvent(RaftEvent e) {
		if (!running) {
			if (e.getType() == EventType.CLIENT_REQUEST) {
				e.getClientSource().send(algo, new ClientResponse(301, null));
			}
			return;
		}
		switch (e.getType()) {
		case APPEND:
			AppendRequest ar = (AppendRequest) e.getEvent();
			boolean appended = algo.append(ar);
			if (ar.getPayload() != null) {
				logger.info("appended: " + appended + " i=" + ar.getIndex() + " t=" + ar.getLogTerm() + " " + algo);
				e.getSource().getChannel()
					.send(algo, new AppendResponse(algo.getTerm().getCurrent(), appended, ar.getIndex(), ar.getLogTerm()));
			} else {
				logger.info("ping received: i=" + ar.getIndex() + " t=" + ar.getLeaderTerm() + " " + algo);
			}
			break;
		case APPEND_RESPONSE:
			algo.handleResponse(e.getSource(), (AppendResponse)e.getEvent());
			logger.info("append responded: " + e.getEvent() + " " + algo);
			break;
		case REQUEST_VOTE:
			RequestForVote rfv = (RequestForVote) e.getEvent();
			if (algo.getElection().vote(e.getSource(), rfv)) {
				logger.info("Voted for: " + rfv);
				e.getSource().getChannel().send(algo, new VoteGranted(rfv.getTerm()));
			} else {
				logger.info("Vote declined: " + rfv);
			}
			break;
		case VOTE_GRANTED:
			algo.getElection().voteReceived((VoteGranted) e.getEvent());
			break;
		case LOOP:
			long l = algo.loop();
			if (l < 1) {
				l = ElectionLogic.PING_LOOP;
			} else if (l > ElectionLogic.PING_LOOP) {
				l = ElectionLogic.PING_LOOP;
			}
			loop(l);
			break;
		case CLIENT_REQUEST:
			try {
				algo.handleClientRequest(e.getClientSource(), e.getEvent());
			} catch (NotLeaderException ex) {
				logger.warn("Not a leader: " + e + ": " + ex, ex);
				e.getClientSource().send(algo, new ClientResponse(301, null));
			}
			break;
		}
	}

	
	@Override
	public void voteReceived(IMember member, VoteGranted vote) {
		queue.getQueue().add(
				new RaftEvent(member, EventType.VOTE_GRANTED, vote));
		executor.execute(queue);
	}

	@Override
	public void append(IMember source, AppendRequest entry) {
		queue.getQueue().add(new RaftEvent(source, EventType.APPEND, entry));
		executor.execute(queue);

	}

	@Override
	public void vote(IMember IMember, RequestForVote vote) {
		queue.getQueue().add(
				new RaftEvent(IMember, EventType.REQUEST_VOTE, vote));
		executor.execute(queue);
	}


	public void handleClientRequest(IMsg client, ClientRequest req)  {
		queue.getQueue().add(new RaftEvent(client, EventType.CLIENT_REQUEST, req.getPayload()));
		executor.execute(queue);
	}
	
	
	
	@Override
	public void appendResponse(IMember source, AppendResponse resp) {
		queue.getQueue().add(new RaftEvent(source, EventType.APPEND_RESPONSE, resp));
		executor.execute(queue);		
	}

	public void stop() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public Algo getAlgo() {
		return algo;
	}

	public int getQueueSize() {
		return queue.getQueue().size();
	}


	
	@Override
	public String getName() {
		return "(" + algo + ")" ;
	}



	private class LoopEvent implements Runnable {
		
		private final AtomicInteger scheduleCounter = new AtomicInteger();
		
		@Override
		public void run() {
			scheduleCounter.decrementAndGet();
			queue.getQueue().add(new RaftEvent((IMember)null, EventType.LOOP, null));
			executor.execute(queue);
		}
		
	}

}
