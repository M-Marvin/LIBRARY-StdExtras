package de.m_marvin.stdextras.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizedLock implements Lock {
	
	private final Set<Thread> readers = new HashSet<>();
	private Thread writer = null;
	
	@Override
	public void lockWrite() throws InterruptedException {
		synchronized (this.readers) {
			if (this.writer == Thread.currentThread())
				throw new IllegalStateException("thread already owns the write lock");
			while (!this.readers.isEmpty() && this.writer == null)
				this.readers.wait();
			this.writer = Thread.currentThread();
		}
	}

	@Override
	public void unlockWrite() {
		synchronized (this.readers) {
			if (this.writer != Thread.currentThread())
				throw new IllegalStateException("thread does not own the write lock");
			this.writer = null;
			this.readers.notify();
		}
	}

	@Override
	public void lockRead() throws InterruptedException {
		synchronized (this.readers) {
			while (this.writer != null)
				this.writer.wait();
			if (!this.readers.add(Thread.currentThread()))
				throw new IllegalStateException("thread already owns a read lock");
		}
	}

	@Override
	public void unlockRead() {
		synchronized (this.readers) {
			if (!this.readers.remove(Thread.currentThread()))
				throw new IllegalStateException("thread does not own a read lock");
			this.readers.notify();
		}
	}
	
	@Override
	public void promoteWrite() throws InterruptedException {
		synchronized (readers) {
			if (this.writer == Thread.currentThread())
				throw new IllegalStateException("thread already owns the write lock");
			while (this.readers.size() != 1 && this.writer == null && this.readers.contains(Thread.currentThread()))
				this.readers.wait();
			this.readers.remove(Thread.currentThread());
			this.writer = Thread.currentThread();
		}
	}
	
	@Override
	public void demoteRead() {
		synchronized (this.readers) {
			if (this.writer != Thread.currentThread())
				throw new IllegalStateException("thread does not own the write lock");
			this.writer = null;
			this.readers.add(Thread.currentThread());
			this.readers.notify();
		}
	}
	
}
