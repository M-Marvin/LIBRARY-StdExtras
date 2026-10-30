import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import de.m_marvin.stdextras.periodic.Delay;
import de.m_marvin.stdextras.periodic.PeriodicExecutorService;
import de.m_marvin.stdextras.periodic.PeriodicThreadPool;

public class Test {
	
	public static void main(String[] args) throws InterruptedException {
		
//		long tstart = System.nanoTime();
//		
//		Delay.sleepus(900000);
////		Thread.sleep(900);
//		
//		System.out.println(System.nanoTime() - tstart);
//		
//		System.exit(0);
		
		
		PeriodicThreadPool executor = new PeriodicThreadPool(1000000, 6, "TestPool");
		

//		{
//			Test test = new Test();
//			executor.submit(test::periodicTask);
//		}
//		
//		Thread.sleep(3000);
//		
//		{
//			Test test = new Test();
//			executor.submit(test::periodicTask);
//		}
//
//		Thread.sleep(3000);
//		
//		{
//			Test test = new Test();
//			executor.submit(test::periodicTask);
//		}
//
//		Thread.sleep(3000);
//		
//		{
//			Test test = new Test();
//			executor.submit(test::periodicTask);
//		}
		
		try {
			for (int i = 0; i < 10; i++) {
				
				Test test = new Test();
				executor.submit(test::periodicTask);
				
				Thread.sleep(2000);
				System.out.println("---------------");
				
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		// just block until manual termination ...
//		synchronized (Test.class) {
//			try {
//				Test.class.wait();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		Thread.sleep(4000);
		
		executor.shutdown();
		if (!executor.awaitTermination(6, TimeUnit.SECONDS))
			executor.shutdownNow();
		
	}
	
	private long t = -1;
	private long d = 30;
	
	public Test() {
	}
	
	public boolean periodicTask(long lastPeriod, BooleanSupplier hasTime) {
		
		if (t != -1) {
			long T = System.currentTimeMillis() - this.t;
			this.t = System.currentTimeMillis();
			
			System.out.println("TEST T=" + T);
		} else {
			this.t = System.currentTimeMillis();
		}
		
		try {
			Thread.sleep(4);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("D " + d--);
		return d <= 0;
//		return new Random().nextBoolean();
		
	}
	
}
