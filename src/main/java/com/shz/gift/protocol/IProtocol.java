package com.shz.gift.protocol;

public interface IProtocol {

	public AppendRequest createAppend(long term, long index, long prevTerm, long prevIndex, Object payload);
	
	
	
}
