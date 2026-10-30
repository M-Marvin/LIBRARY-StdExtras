package de.m_marvin.stdextras.periodic;

import java.util.function.BooleanSupplier;

/**
 * A {@link Thread} executing {@link PeriodicRunnable} in a configurable periodic loop.
 */
public class PeriodicThread extends Thread implements PeriodicRunnable {
	
	private final PeriodicRunnable target;
	private final LoopTimer timer;
	private volatile boolean halt;
	
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }
	
	public PeriodicThread(long periode) {
		this((PeriodicRunnable) null, periode);
	}

	public PeriodicThread(PeriodicRunnable target, long periode, String name) {
		this((ThreadGroup) null, target, periode, name);
	}

	public PeriodicThread(PeriodicRunnable target, long periode) {
		this((ThreadGroup) null, target, periode);
	}

	public PeriodicThread(String name, long periode) {
		this((ThreadGroup) null, periode, name);
	}

	public PeriodicThread(ThreadGroup group, PeriodicRunnable target, long periode, String name, long stackSize, boolean inheritThreadLocals) {
		super(group, null, name, stackSize, inheritThreadLocals);
		this.target = target;
		this.timer = new LoopTimer(periode);
	}

	public PeriodicThread(ThreadGroup group, PeriodicRunnable target, long periode, String name, long stackSize) {
		this(group, target, periode, name, stackSize, false);
	}

	public PeriodicThread(ThreadGroup group, PeriodicRunnable target, long periode, String name) {
		this(group, target, periode, name, 0);
	}

	public PeriodicThread(ThreadGroup group, PeriodicRunnable target, long periode) {
		this(group, target, periode, "PeriodicThread-" + nextThreadNum());
	}

	public PeriodicThread(ThreadGroup group, long periode, String name) {
		this(group, (PeriodicRunnable) null, periode, name);
	}
	
	/**
	 * Returns the internal loop timer regulation the threads periodic execution.
	 * @return the loop timer instance of this thread
	 */
	public LoopTimer getTimer() {
		return timer;
	}
	
	/**
	 * Returns an estimate of how much time the thread spends doing actual work vs sleeping based on the loop time statistics.<br>
	 * - 0.0 equals no work being done (100% sleep time)<br>
	 * - 1.0 means the thread spends all its time performing work<br> 
	 * - values greater than 1.0 indicate the thread is overloaded and can't keep up with the requested loop cycle time
	 * @return a float value indicating the load factor
	 */
	public float getLoadFactor() {
		return this.timer.getLoopTime() / (float) this.timer.getLoopTargetTime();
	}
	
	@Override
	public void run() {
		long periodTime = 0;
		this.timer.reset();
		while (!halt) {
			this.halt = runTick(periodTime, this.timer::hasTime);
			periodTime = this.timer.loop();
		}
	}
	
	public boolean isHalted() {
		return halt;
	}
	
	/**
	 * Requests the thread to stop executing the periodic runnable after completion of the current cycle.
	 */
	public void halt() {
		this.halt = true;
	}
	
	/**
	 * Requests the thread to stop executing the periodic runnable after completition of the current cycle, then waits for the threads termination.
	 * @throws InterruptedException if any thread has interrupted the current thread. The interrupted status of the current thread is cleared when this exception is thrown.
	 */
	public void haltAndJoin() throws InterruptedException {
		halt();
		join();
	}
	
	@Override
	public boolean runTick(long lastPeriod, BooleanSupplier hasTime) {
		if (this.target != null) {
			return this.target.runTick(lastPeriod, hasTime);
		}
		return false;
	}
	
}
