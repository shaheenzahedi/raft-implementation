package com.shz.gift.algotest;
import com.shz.gift.handler.ICommitHandler;
import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.ClientResponse;

public class RespondingICommitHandler implements ICommitHandler {

    @Override
    public void commit(AppendRequest entry) {
        Object o = commitImpl(entry);
        if (entry.getClientChannel() != null) {
            entry.getClientChannel().send(null, o);
        }
    }

    protected Object commitImpl(AppendRequest entry) {
        return new ClientResponse(0, null);
    }

    @Override
    public void reject(AppendRequest entry) {
        if (entry.getClientChannel() != null) {
            entry.getClientChannel().send(null, new ClientResponse(1, null));
        }
    }


}
