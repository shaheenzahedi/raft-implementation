package com.shz.gift.log;

public interface IEntry {

	public long index();
	
	public long term();
	
	public long previousIndex();
	
	public long previousTerm();
	
	public Object payload();
	
}
