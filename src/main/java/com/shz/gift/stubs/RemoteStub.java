package com.shz.gift.stubs;

import com.shz.gift.ClusterMember;
import com.shz.gift.Raft;
import com.shz.gift.RaftEventQueue;
import com.shz.gift.protocol.*;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteStub implements ClusterMember {

    private final ScheduledExecutorService executor;
    private RemoteStub routeBack;
    private final Random random = new Random();

    public RaftEventQueue getRaftListener() {
        return raftListener;
    }

    private final RaftEventQueue raftListener;

    private final MessageChannel channel = new ChannelStub();

    private long index;
    private long match;

    private long lastCmd = System.currentTimeMillis();

    public RemoteStub(RaftEventQueue raftListener, ScheduledExecutorService executor) {
        super();
        this.raftListener = raftListener;
        this.executor = executor;
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
    public MessageChannel getChannel() {
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

    private class ChannelStub implements MessageChannel {

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
                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        raftListener.vote(routeBack, (RequestForVote) o);
                    }
                }, random.nextInt(delay) + 1, TimeUnit.MILLISECONDS);
            } else if (o instanceof VoteGranted) {
                VoteGranted r = (VoteGranted) o;
                raftListener.voteReceived(routeBack, r);
            } else {
                System.err.println("Unknown cmd: " + o);
            }
        }

    }

}