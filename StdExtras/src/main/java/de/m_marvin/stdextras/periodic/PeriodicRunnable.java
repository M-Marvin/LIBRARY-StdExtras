package de.m_marvin.stdextras.periodic;

import java.util.function.BooleanSupplier;

/**
 * A {@link Runnable} but for periodic execution.
 * It provides two additional parameters each time it is run:<br>
 * - The time since the last execution cycle (the period of the loop calling it)<br>
 * - A boolean supplier which can be used as indicator weather the targeted execution time has been exceeded
 */
@FunctionalInterface
public interface PeriodicRunnable {
	
	/**
	 * @param periode time in total the last execution period took (including eventual idle time to reach the target duration)<br>
	 * @param hasTime a boolean supplier which will return true as long as the execution time has not exceeded the target period time for this tick.
	 * @return true if the runnable is completed and no further calls should be made
	 */
	public boolean runTick(long lastPeriod, BooleanSupplier hasTime);
	
}
