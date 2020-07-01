package com.shz.gift.executor;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class QueueTask<E> implements Runnable {


	private final Queue<E> queue = new LinkedBlockingQueue<>();
	
	private final AtomicBoolean running = new AtomicBoolean(false);
	
	private final Lock lock = new ReentrantLock();
	
	private final Handler<E> handler;
	
	public QueueTask(Handler<E> handler) {
		super();
		this.handler = handler;
	}


	@Override
	public void run() {
		String oldName = Thread.currentThread().getName();
		Thread.currentThread().setName("" + handler.getName());
		while (true) {
			E o = queue.poll();
			if (o == null) {
				lock.lock();
				try {
					if (queue.isEmpty()) {
						running.set(false);
						Thread.currentThread().setName(oldName);
						return;
					} else {
						continue;
					}
				} finally {
					lock.unlock();
				}
			}
			try {
				handler.handleEvent(o);
			} catch (Exception e) {
				System.err.println("Failed to handle: " + o + ": " + e);
			}			
		}
	}


	public AtomicBoolean getRunning() {
		return running;
	}

	public Lock getLock() {
		return lock;
	}


	public Queue<E> getQueue() {
		return queue;
	}
	
}
