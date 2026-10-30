package de.m_marvin.stdextras.concurrent;
import java.util.concurrent.atomic.AtomicInteger;

public class SpinLock implements Lock {
	
	private final AtomicInteger state = new AtomicInteger(0);
	
	@Override
	public void lockWrite() {
		do {
			while (this.state.get() != 0)
				Thread.onSpinWait();
		} while (!this.state.compareAndSet(0, -1));
	}
	
	@Override
	public void unlockWrite() {
		do {
			if (this.state.get() != -1)
				throw new IllegalStateException("double unlock, no write lock left");
		} while (!this.state.compareAndSet(-1, 0));
	}

	@Override
	public void lockRead() {
		int readers;
		do {
			while ((readers = this.state.get()) < 0)
				Thread.onSpinWait();
		} while (!this.state.compareAndSet(readers, readers + 1));
	}

	@Override
	public void unlockRead() {
		int readers;
		do {
			if ((readers = this.state.get()) <= 0)
				throw new IllegalStateException("double unlock, no read locks left");
		} while (!this.state.compareAndSet(readers, readers - 1));
	}
	
	@Override
	public void promoteWrite() throws InterruptedException {
		do {
			while (this.state.get() != 1)
				Thread.onSpinWait();
		} while (!this.state.compareAndSet(1, -1));
	}
	
	@Override
	public void demoteRead() {
		do {
			if (this.state.get() != -1)
				throw new IllegalStateException("double unlock, no write lock left");
		} while (!this.state.compareAndSet(-1, 1));
	}
	
}
