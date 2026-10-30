package de.m_marvin.stdextras.periodic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import de.m_marvin.stdextras.comparable.ComparatorExtras;
import de.m_marvin.stdextras.concurrent.Lock;
import de.m_marvin.stdextras.concurrent.SpinLock;

public class PeriodicThreadPool implements PeriodicExecutorService {
	
	public static final float THREAD_TAGET_LAOD_FACTOR = 0.75F;
	
	private volatile long periodeTime;
	private volatile boolean inShutdown = false;
	private final int maxPoolSize;
	private final String workerNamePrefix;
	private final Lock workersLock = new SpinLock();
	private final List<PeriodicWorkerThread> workers;
	private final PeriodicWorkerThreadFactory threadFactory;
	
	@FunctionalInterface
	public static interface PeriodicWorkerThreadFactory {
		public PeriodicWorkerThread newThread(PeriodicThreadPool pool);
	}
	
	public static class PeriodicWorkerThread extends PeriodicThread {
		
		private final PeriodicThreadPool pool;
		private final LongSupplier periodeTime;
		private final Lock taskLock = new SpinLock();
		private final List<PeriodicTask> tasks = new ArrayList<PeriodicTask>();
		private volatile boolean mayTerminate = false;
		
		public PeriodicWorkerThread(PeriodicThreadPool pool, LongSupplier periodeTime) {
			super(periodeTime.getAsLong());
			this.pool = pool;
			this.periodeTime = periodeTime;
		}
		
		public PeriodicWorkerThread(PeriodicThreadPool pool, String name, LongSupplier periodeTime) {
			super(name, periodeTime.getAsLong());
			this.pool = pool;
			this.periodeTime = periodeTime;
		}
		
		public PeriodicWorkerThread(PeriodicThreadPool pool, ThreadGroup group, LongSupplier periodeTime, String name) {
			super(group, periodeTime.getAsLong(), name);
			this.pool = pool;
			this.periodeTime = periodeTime;
		}
		
		public boolean insertTask(PeriodicTask task) throws InterruptedException {
			// lock onto the task list in read mode first, to prevent other write locks to take effect until this is done
			this.taskLock.lockReadThenWrite();
			boolean insert = !this.mayTerminate && !this.tasks.contains(task);
			if (insert)
				this.tasks.add(task);
			this.taskLock.unlockWrite();
			return insert;
		}
		
		public boolean removeTask(PeriodicTask task) throws InterruptedException {
			this.taskLock.lockWrite();
			boolean removed = this.tasks.remove(task);
			this.taskLock.unlockWrite();
			return removed;
		}
		
		public boolean suspendTask(String name) throws InterruptedException {
			this.taskLock.lockRead();
			for (var task : this.tasks) {
				if (name.equals(task.getName())) {
					try {
						this.taskLock.promoteWrite();
						this.tasks.remove(task);
						this.taskLock.unlockWrite();
						return true;
					} finally {
						this.taskLock.unlockRead();
					}
				}
			}
			this.taskLock.unlockRead();
			return false;
		}
		
		@Override
		public boolean runTick(long lastPeriod, BooleanSupplier hasTime) {
			
			long periodTime = periodeTime.getAsLong();
			if (getTimer().getLoopTargetTime() != periodTime)
				getTimer().setLoopTargetTime(periodTime);
			
//			System.out.println(getTimer() + " TASKS: " + this.tasks.size() + " LP: " + lastPeriod);
			
			try {
				this.taskLock.lockRead();
				Iterator<PeriodicTask> taskIter = this.tasks.iterator();
				while (taskIter.hasNext()) {
					boolean completed = taskIter.next().runTick(lastPeriod, hasTime);
					if (completed) {
						try {
							this.taskLock.promoteWrite();
							taskIter.remove();
							this.taskLock.demoteRead();
						} catch (InterruptedException e) {
							this.taskLock.unlockRead();
							return true; // terminate periodic execution
						}
					}
				}
				this.mayTerminate = this.tasks.isEmpty();
				this.taskLock.unlockRead();
				
				if (this.mayTerminate) {
					// claim a write lock and double check, the write lock ensures the insert method has finished and potential new tasks have been inserted
					this.taskLock.lockWrite();
					this.mayTerminate = this.tasks.isEmpty();
					this.taskLock.unlockWrite();
					
					if (this.mayTerminate) {
						this.pool.workersLock.lockWrite();
						this.pool.workers.remove(this);
						this.pool.workersLock.unlockWrite();
					}
				}
				return this.mayTerminate;
			} catch (InterruptedException e) {
				return true; // terminate periodic execution
			}
		}
		
	}
	
	public PeriodicThreadPool(long periodeTime, int maxPoolSize) {
		this(periodeTime, maxPoolSize, null);
	}
	
	public PeriodicThreadPool(long periodeTime, int maxPoolSize, String workerNamePrefix) {
		this(periodeTime, maxPoolSize, null, workerNamePrefix);
	}
	
	public PeriodicThreadPool(long periodeTime, int maxPoolSize, PeriodicWorkerThreadFactory threadFactory, String workerNamePrefix) {
		if (periodeTime <= 0)
			throw new IllegalArgumentException("loop time must be greater than zero");
		if (maxPoolSize <= 0)
			throw new IllegalArgumentException("max pool size must be greater than zero");
		
		this.periodeTime = periodeTime;
		this.workerNamePrefix = workerNamePrefix;
		this.maxPoolSize = maxPoolSize;
		this.workers = new ArrayList<PeriodicWorkerThread>(maxPoolSize);
		
		if (threadFactory != null)
			this.threadFactory = threadFactory;
		else
			this.threadFactory = pool -> new PeriodicWorkerThread(this, null, this::getPeriodeTime, nextWorkerThreadName());
	}
	
	private final AtomicInteger workerNameIndex = new AtomicInteger();
	
    public final String nextWorkerThreadName() {
        String prefix = workerNamePrefix;
        int tid = this.workerNameIndex.getAndIncrement();
        if (prefix == null) // commonPool has no prefix
            prefix = "PeriodicTaskPool-Worker-";
        return prefix.concat(Integer.toString(tid));
    }
	
	public long getPeriodeTime() {
		return periodeTime;
	}
	
	public void setPeriodeTime(long periodeTime) {
		if (this.periodeTime <= 0)
			throw new IllegalArgumentException("loop time must be greater than zero");
		this.periodeTime = periodeTime;
		
	}
	
	@Override
	public void submit(PeriodicRunnable runnable) {
		Objects.requireNonNull(runnable, "runnable");
		if (this.inShutdown)
			throw new RejectedExecutionException("periodic executor is in shuting down");
		
		PeriodicTask task;
		if (runnable instanceof PeriodicTask runnableTask)
			task = runnableTask;
		else
			task = new PeriodicTask(runnable);
		
		try {
			this.workersLock.lockRead();
			boolean canPoolGrow = this.maxPoolSize > this.workers.size();
			try {
				do {
					Iterator<PeriodicWorkerThread> workers = this.workers.stream()
							.sorted(ComparatorExtras.toDouble(PeriodicWorkerThread::getLoadFactor)).iterator();
					while (workers.hasNext()) {
						var worker = workers.next();
						if ((!canPoolGrow || worker.getLoadFactor() < THREAD_TAGET_LAOD_FACTOR) && worker.insertTask(task)) {
							this.workersLock.unlockRead();
							return;
						}
					}
				} while (!canPoolGrow);
				
				this.workersLock.promoteWrite();
				try {
					PeriodicWorkerThread newWorker = this.threadFactory.newThread(this);
					newWorker.insertTask(task);
					this.workers.add(newWorker);
					newWorker.start();
					return;
				} finally {
					this.workersLock.unlockWrite();
				}
				
			} catch (InterruptedException e) {
				this.workersLock.unlockRead();
			}
		} catch (InterruptedException e) {
			throw new RejectedExecutionException("thread interrupted during execution submission", e);
		}
	}
	
	@Override
	public boolean suspend(String name) {
		Objects.requireNonNull(name, "name");
		
		try {
			this.workersLock.lockRead();
			boolean suspended = false;
			for (var worker : this.workers) {
				if (worker.suspendTask(name)) {
					suspended = true;
					break;
				}
			}
			this.workersLock.unlockRead();
			return suspended;
		} catch (InterruptedException e) {
			throw new RejectedExecutionException("thread interrupted during execution suspension", e);
		}
	}
	
	@Override
	public void shutdown() {
		this.inShutdown = true;
	}

	@Override
	public List<PeriodicRunnable> shutdownNow() {
		try {
			this.workersLock.lockRead();
			List<PeriodicRunnable> tasks = this.workers.stream()
				.peek(PeriodicWorkerThread::halt)
				.flatMap(worker -> {
					try {
						worker.taskLock.lockRead();
						List<PeriodicRunnable> taskCopy = new ArrayList<>(worker.tasks);
						worker.taskLock.unlockRead();
						return taskCopy.stream();
					} catch (InterruptedException e) {
						return Stream.empty();
					}
				})
				.toList();
			this.workersLock.unlockRead();
			return tasks;
		} catch (InterruptedException e) {
			return Collections.emptyList();
		}
	}
	
	@Override
	public boolean isShutdown() {
		return this.inShutdown;
	}

	@Override
	public boolean isTerminated() {
		if (!this.isShutdown())
			return false;
		return this.workers.isEmpty() || this.workers.stream().filter(Thread::isAlive).count() == 0;
	}
	
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		long tend = (int) (System.currentTimeMillis() + unit.toMillis(timeout));
		while (!isTerminated()) {
			long tremaining = tend - System.currentTimeMillis();
			if (tremaining < 0)
				return false;
			else
				Thread.sleep(Math.min(this.periodeTime, tremaining));
		}
		return true;
	}
	
}
