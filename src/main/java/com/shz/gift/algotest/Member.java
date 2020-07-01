package com.shz.gift.algotest;

import com.shz.gift.preps.EventQueue;

import java.util.ArrayList;
import java.util.List;

public class Member {
    private final EventQueue raftListener;
    private final List<RemoteStub> remotes = new ArrayList<>();

    public Member(EventQueue queue) {
        super();
        this.raftListener = queue;
    }

    public EventQueue getRaftListener() {
        return raftListener;
    }

    public List<RemoteStub> getRemotes() {
        return remotes;
    }
}
