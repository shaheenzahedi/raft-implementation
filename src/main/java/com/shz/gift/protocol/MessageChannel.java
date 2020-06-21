package com.shz.gift.protocol;

import com.shz.gift.Raft;

public interface MessageChannel {

	public void send(Raft source, Object o);
	
}
