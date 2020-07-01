package com.shz.gift.stubs;

import com.shz.gift.Raft;
import com.shz.gift.protocol.ClientResponse;
import com.shz.gift.protocol.MessageChannel;

import java.util.concurrent.atomic.AtomicInteger;

public class Request implements MessageChannel {

    private final AtomicInteger successCounter;
    private final AtomicInteger errCounter;



    Object payload = new Object();
    int err = -1;

    public Request(AtomicInteger successCounter, AtomicInteger errCounter) {
        this.successCounter = successCounter;
        this.errCounter = errCounter;
    }

    @Override
    public void send(Raft r, Object o) {
        ClientResponse resp = (ClientResponse) o;
        err = resp.getErrCode();
        if (err == 0) {
            successCounter.incrementAndGet();
        } else {
            errCounter.incrementAndGet();
        }
    }

    public Object getPayload() {
        return payload;
    }

    public int getErr() {
        return err;
    }
}
