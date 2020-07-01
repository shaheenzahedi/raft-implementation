package com.shz.gift.log;

import com.shz.gift.handler.ICommitHandler;
import com.shz.gift.protocol.AppendRequest;

public interface ILog {
	
	public boolean append(AppendRequest log);
	
	public AppendRequest get(long index);
	
	public long getLastCommitIndex();
	
	public long getLastIndex();
	
	public long getLastTerm();
	
	public void commit(long index);
 
	public void setCommitHandler(ICommitHandler cm);
	public void setLogWriter(IWriter writer);
}
