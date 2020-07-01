package com.shz.gift.protocol;

import com.shz.gift.preps.Raft;

public interface IMsg {

	public void send(Raft source, Object o);
	
}
