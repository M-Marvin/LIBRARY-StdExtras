package de.m_marvin.stdextras.concurrent;

public interface Lock {
	
	public void lockWrite() throws InterruptedException;
	public void unlockWrite();
	
	public void promoteWrite() throws InterruptedException;
	public void demoteRead();
	
	public void lockRead() throws InterruptedException;
	public void unlockRead();
	
	default public void lockReadThenWrite() throws InterruptedException {
		lockRead();
		promoteWrite();
	}
	
}
