package com.shz.gift.executor;

public interface Handler<E> {

	public void handleEvent(E o);

	public String getName();
	
}
