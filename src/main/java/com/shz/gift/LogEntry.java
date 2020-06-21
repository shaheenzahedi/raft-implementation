package com.shz.gift;

public interface LogEntry {

	public long index();
	
	public long term();
	
	public long previousIndex();
	
	public long previousTerm();
	
	public Object payload();
	
}
