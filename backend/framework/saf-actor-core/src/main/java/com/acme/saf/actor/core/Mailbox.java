package com.acme.saf.actor.core;


public interface Mailbox{
	
	void enqueue(Message message);
	
	Message dequeue();
	
	boolean isEmpty();
	
	int size();
	
	void clear();
	
	
	
	
}