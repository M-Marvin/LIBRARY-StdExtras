package de.m_marvin.stdextras.periodic;

public class Delay {
	
	public static void sleepms(int micros) throws InterruptedException {
		sleepus(micros * 1000);
	}
	
	private static final long THREAD_SLEEP_MS_BUFFER = 2;
	
	public static void sleepus(long micros) throws InterruptedException {
		long tend = System.nanoTime() + micros * 1000;
		long tms = Math.floorDiv(micros, 1000) - THREAD_SLEEP_MS_BUFFER;
		Thread.sleep(tms);
		long tremaining = System.nanoTime() - tend;
		if (tremaining > 0)
			sleepns(tremaining);
	}
	
	public static void sleepns(long nanos) throws InterruptedException {
		long tend = System.nanoTime() + nanos;
		while (System.nanoTime() < tend)
			Thread.onSpinWait();
	}
	
}
