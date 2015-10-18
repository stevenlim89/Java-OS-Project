package nachos.threads;

import java.util.PriorityQueue;
import java.util.*; // added in to get Map 

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		//check queue if thread is ready to wake up, check the wake time 
		//loop through the times pq to compare the time and wake up the thread
		if(pq.size() != 0){
			if(pq.peek()<=Machine.timer().getTime()){
				pq.poll();
				s.V();
			//KThread.currentThread().ready();
			}
		}
		//KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		// busy waitingwhile (wakeTime > Machine.timer().getTime()){		}
		
		//notes
		//sleep in waituntil, wake up in timer interrupt
		// timer interrupt will tick 500 times, check if threads need to be waken up
		// never wake up current (main). make current threadsleep
		if(x <= 0)
			return;
		
		long wakeTime = Machine.timer().getTime() + x; // block thread 
		//that is calling waituntil
		pq.add(Map<wakeTime,s>); //stores waketime 
		s.P(); //TODO: add s to the priority queue before calling P
	}
	public static void selfTest() {
	    KThread t1 = new KThread(new Runnable() {
	        public void run() {
	            long time1 = Machine.timer().getTime();
	            int waitTime = 10000;
	            System.out.println("Thread calling wait at time:" + time1);
	            ThreadedKernel.alarm.waitUntil(waitTime);
	            System.out.println("Thread woken up after:" + (Machine.timer().getTime() - time1));
	            Lib.assertTrue((Machine.timer().getTime() - time1) > waitTime, " thread woke up too early.");
	            
	        }
	    });
	    t1.setName("T1");
	    t1.fork();
	    t1.join();
	}
	private PriorityQueue<> pq = new PriorityQueue<Map<Long,Semaphore>>();
	private static Semaphore s =new Semaphore(0);
}
