package com.shz.gift;

import com.shz.gift.protocol.MessageChannel;


public interface ClusterMember {

	public MessageChannel getChannel();
	
	public long getNextIndex();
	
	public void setNextIndex(long index);
	
	public long getMatchIndex();

	public void setMatchIndex(long matchIndex);

	public long getLastCommandReceived();
	
	public void setLastCommandReceived(long time);
}
