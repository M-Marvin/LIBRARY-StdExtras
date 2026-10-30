package de.m_marvin.stdextras.periodic;

public class LoopTimer {
	
	public static final int DEFAULT_AVERAGING_SAMPLE_COUNT = 10;
	
	private long loopTime;
	private int averageingCount = DEFAULT_AVERAGING_SAMPLE_COUNT;
	
	public LoopTimer(long loopTime) {
		if (loopTime <= 0)
			throw new IllegalArgumentException("loop time must be greater than zero");
		this.loopTime = loopTime;
	}

	/**
	 * Sets the configured loop time this timer attempts to achieve.
	 * @param loopTime the configured loop time in us
	 */
	public void setLoopTargetTime(long loopTime) {
		if (loopTime <= 0)
			throw new IllegalArgumentException("loop time must be greater than zero");
		this.loopTime = loopTime;
	}
	
	/**
	 * Gets the configured loop time this timer attempts to achieve.
	 * @return the configured loop time in us
	 */
	public long getLoopTargetTime() {
		return this.loopTime;
	}
	
	/**
	 * Configures how many samples are taken to compute the average statistics for this timer.
	 * @param averageCount the number of samples to take
	 */
	public void setAveragingCount(int averageingCount) {
		if (averageingCount <= 0)
			throw new IllegalArgumentException("averaging count must be greater than zero");
		this.averageingCount = averageingCount;
	}
	
	/**
	 * The average time it takes for the work of a loop cycle to be carried out.
	 * This should always be smaller than the configured loop time, otherwise indicates the work takes to long for the desired loop time to be achieved.
	 * @return the time the work portion of every loop cycles takes in us
	 */
	public int getLoopTime() {
		if (this.averageCount == 0)
			return (int) this.loopTime;
		return this.averageLoopTime / this.averageCount;
	}

	/**
	 * The average time a complete loop cycle, including the waiting time takes.
	 * This should ideally always match the configured loop time, but may deviate to some extend depending on the accuracy of the system clock, or might be significantly longer if the work carried out by the loop takes a long time.
	 * @return the time a complete loop cycle takes in us
	 */
	public int getLoopTimeTotal() {
		if (this.averageCount == 0)
			return (int) this.loopTime;
		return this.averageLoopTimeTotal / this.averageCount;
	}
	
	private long lastLoopTime = -1L;
	private int averageLoopTime = 0;
	private int averageLoopTimeTotal = 0;
	private int averageCount = 0;
	
	/**
	 * Resets the timer state including all statistics.<br>
	 * This function should be called right before the start of the first loop cycle to ensure a consistent starting condition.
	 */
	public void reset() {
		this.lastLoopTime = System.nanoTime() / 1000;
		this.averageLoopTime = 0;
		this.averageLoopTimeTotal = 0;
		this.averageCount = 0;
	}
	
	/**
	 * Check weather the current loop cycle has still time to do work without exceeding the period duration configured with this timer.
	 * @return true if there is still time left to do work without exceeding the period duration.
	 */
	public boolean hasTime() {
		return (System.nanoTime() / 1000 - this.lastLoopTime) < this.loopTime;
	}
	
	/**
	 * Calling this method at the end of each loop cycle will estimate the remaining time left of this period, and will try to burn that time by suspending the thread for this amount of time.
	 * @return the time delta between this loop cycle and the last (aka the time between the last loop call and this one) in us
	 */
	public long loop() {
		
		long time = System.nanoTime() / 1000;
		long delta = time - this.lastLoopTime;
		if (delta < this.loopTime) {
			long delay = (this.loopTime - delta); // / 1000;
			try {
				if (delay > 0)
					Delay.sleepus(delay);
					//Thread.sleep(delay);
			} catch (InterruptedException e) {}
		}
		
		long loopTime = System.nanoTime() / 1000;
		long deltaTotal = loopTime - this.lastLoopTime;
		this.averageLoopTime += delta;
		this.averageLoopTimeTotal += deltaTotal;
		this.averageCount++;
		if (this.averageCount > this.averageingCount) {
			this.averageLoopTime -= this.averageLoopTime / this.averageCount;
			this.averageLoopTimeTotal -= this.averageLoopTimeTotal / this.averageCount;
			this.averageCount--;
		}
		this.lastLoopTime = loopTime;
		
		return deltaTotal;
	}
	
	@Override
	public String toString() {
		return "LoopTimer{Ttarget=" + getLoopTargetTime() + ", Taverage=" + getLoopTimeTotal() + ", Twork=" + getLoopTime() + "}";
	}
	
}