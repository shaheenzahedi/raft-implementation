package com.shz.gift.algotest;

import com.shz.gift.preps.RaftEventQueue;

import java.util.ArrayList;
import java.util.List;

public class Member {
    private final RaftEventQueue raftListener;
    private final List<RemoteStub> remotes = new ArrayList<>();

    public Member(RaftEventQueue queue) {
        super();
        this.raftListener = queue;
    }

    public RaftEventQueue getRaftListener() {
        return raftListener;
    }

    public List<RemoteStub> getRemotes() {
        return remotes;
    }
}
