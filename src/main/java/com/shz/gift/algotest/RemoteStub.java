package com.shz.gift.algotest;

import com.shz.gift.preps.IMember;
import com.shz.gift.preps.Raft;
import com.shz.gift.preps.RaftEventQueue;
import com.shz.gift.protocol.*;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteStub implements IMember {

    private final ScheduledExecutorService executor;
    private final Logger logger;
    private RemoteStub routeBack;
    private final Random random = new Random();

    public RaftEventQueue getRaftListener() {
        return raftListener;
    }

    private final RaftEventQueue raftListener;

    private final IMsg channel = new ChannelStub();

    private long index;
    private long match;

    private long lastCmd = System.currentTimeMillis();

    public RemoteStub(RaftEventQueue raftListener, Logger logger, ScheduledExecutorService executor) {
        super();
        this.raftListener = raftListener;
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    public long getLastCommandReceived() {
        return lastCmd;
    }

    @Override
    public void setLastCommandReceived(long time) {
        this.lastCmd = time;
    }

    @Override
    public IMsg getChannel() {
        return channel;
    }

    @Override
    public long getNextIndex() {
        return index;
    }

    @Override
    public void setNextIndex(long index) {
        this.index = index;
    }

    @Override
    public long getMatchIndex() {
        return match;
    }

    @Override
    public void setMatchIndex(long matchIndex) {
        this.match = matchIndex;
    }

    public String toString() {
        return "Member-" + raftListener.getRaft();
    }

    public void setRouteBack(RemoteStub routeBack) {
        this.routeBack = routeBack;
    }

    private class ChannelStub implements IMsg {

        @Override
        public void send(final Raft source, final Object o) {

            int delay = 25;
            if (o instanceof AppendRequest) {
                AppendRequest r = (AppendRequest) o;
                final AppendRequest req = new AppendRequest(r.getLogTerm(), r.getIndex(),
                        r.getLeaderTerm(), r.getPreviousIndex(),
                        r.getPreviousTerm(), r.getCommitIndex(),
                        r.getPayload());
                executor.schedule(() -> raftListener.append(routeBack, req), random.nextInt(delay) + 1, TimeUnit.MILLISECONDS);
            } else if (o instanceof AppendResponse) {
                AppendResponse resp = (AppendResponse) o;
                raftListener.appendResponse(routeBack, resp);
            } else if (o instanceof RequestForVote) {
                executor.schedule(() -> raftListener.vote(routeBack, (RequestForVote) o), random.nextInt(delay) + 1, TimeUnit.MILLISECONDS);
            } else if (o instanceof VoteGranted) {
                VoteGranted r = (VoteGranted) o;
                raftListener.voteReceived(routeBack, r);
            } else {
                System.err.println("Unknown cmd: " + o);
            }
        }

    }

}