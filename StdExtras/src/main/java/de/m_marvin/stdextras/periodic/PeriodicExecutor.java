package de.m_marvin.stdextras.periodic;

import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;

public interface PeriodicExecutor {
	
	/**
	 * Submits the task for periodic execution to this executor.
	 * @param runnable the runnable to execute
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
	 */
	public void submit(PeriodicRunnable runnable);
	
	/**
	 * Suspends all tasks with the specified name from execution after the current cycle completes, then removes them from the list of tasks.<br>
	 * Only tasks of the type {@link PeriodicTask} can have a name, thus only such tasks can be suspended by this interface.
	 * @param name the name of the task to suspend.
	 * @return true of at least one task was found under the name and suspended
     * @throws NullPointerException if command is null
	 */
	public boolean suspend(String name);
	
	/**
	 * A extension of {@link PeriodicRunnable} which keeps additional statistics over execution time, and can be given a name for identification.<br>
	 * The name will be included in the stacktrace thrown by this task, further more the name allows to task to be identified and suspended from execution trough the executor.
	 */
    public static class PeriodicTask implements PeriodicRunnable {

    	private final PeriodicRunnable target;
    	private final String name;
		private int executionTime;
		private int averageExecutionTime;
		private int averageCount;
		private int averageingCount;

	    private static int taskInitNumber;
	    private static synchronized int nextTaskNum() {
	        return taskInitNumber++;
	    }
		
		public PeriodicTask() {
			this(LoopTimer.DEFAULT_AVERAGING_SAMPLE_COUNT);
		}
		
		public PeriodicTask(int averagingCount) {
			this(null, averagingCount, "PeriodicTask-" + nextTaskNum());
		}
		
		public PeriodicTask(PeriodicRunnable target) {
			this(target, LoopTimer.DEFAULT_AVERAGING_SAMPLE_COUNT, "PeriodicTask-" + nextTaskNum());
		}

		public PeriodicTask(String name) {
			this(LoopTimer.DEFAULT_AVERAGING_SAMPLE_COUNT, name);
		}
		
		public PeriodicTask(int averagingCount, String name) {
			this(null, averagingCount, name);
		}
		
		public PeriodicTask(PeriodicRunnable target, String name) {
			this(target, LoopTimer.DEFAULT_AVERAGING_SAMPLE_COUNT, name);
		}
		
		public PeriodicTask(PeriodicRunnable target, int averagingCount, String name) {
			this.target = target;
			this.name = name;
			this.executionTime = 0;
			this.averageCount = averagingCount;
		}
		
		public void setAverageCount(int averageCount) {
			this.averageCount = averageCount;
		}
		
		public int getAverageCount() {
			return averageCount;
		}
		
		public int getAverageExecutionTime() {
			return this.averageExecutionTime / this.averageCount / 1000;
		}
		
		public String getName() {
			return name;
		}
    	
		@Override
		public final boolean runTick(long lastPeriod, BooleanSupplier hasTime) {
			try {
				long tstart = System.nanoTime();
				boolean completed;
				if (this.target != null)
					completed = this.target.runTick(lastPeriod, hasTime);
				else
					completed = exec(lastPeriod, this.executionTime, hasTime);
				
				this.executionTime = (int) (System.nanoTime() - tstart);
				this.averageExecutionTime += this.executionTime;
				this.averageCount++;
				if (this.averageCount > this.averageingCount) {
					this.executionTime -= this.executionTime / this.averageCount;
					this.averageCount--;
				}
				return completed;
			} catch (Throwable e) {
				throw new PeriodicExecutionException("exception was thrown during periodic task execution: " + getName(), e);
			}
		}
		
		/**
		 * Called when there is no {@link PeriodicRunnable} configured for this task, should be overridden by subclasses implementing an {@link PeriodicTask}
		 * @param periode time in total the last execution period took (including eventual idle time to reach the target duration)<br>
		 * @param lastExecution time it took last time to execute this specific task<br>
		 * @param hasTime a boolean supplier which will return true as long as the execution time has not exceeded the target period time for this tick.
		 * @return true if the runnable is completed and no further calls should be made
		 */
		public boolean exec(long lastPeriod, long lastExecution, BooleanSupplier hasTime) {
			return true;
		}
		
		@Override
		public String toString() {
			return "PeriodicTask{name = " + getName() + ", target = " + this.target + "}";
		}
		
    }
    
}
