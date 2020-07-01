package com.shz.gift.stubs;

import com.shz.gift.example.RespondingCommitHandler;
import com.shz.gift.protocol.AppendRequest;

import java.util.HashSet;
import java.util.Set;

public class HashSetCommits extends RespondingCommitHandler {
    private final Set<Object> commitSet = new HashSet<>();

    @Override
    protected Object commitImpl(AppendRequest entry) {
        commitSet.add(entry.getPayload());
        return super.commitImpl(entry);
    }

    public Set<Object> getCommitSet() {
        return commitSet;
    }
}