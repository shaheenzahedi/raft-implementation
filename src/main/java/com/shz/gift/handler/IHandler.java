package com.shz.gift.handler;

public interface IHandler<E> {

	public void handleEvent(E o);

	public String getName();
	
}
