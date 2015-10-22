package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		//if one of the condition variable queues is empty, sleep
		
		//increment the counter for the waitqueues
		speakCount++;
		System.out.print("speakCount is" + speakCount);

		while(speakCount == 0 || listenCount == 0) {
			System.out.println(KThread.currentThread().getName() + " " + "speaker put to sleep");
			speakEmpty.sleep();
		}
	
		
		//how to transfer control?
		//use while loop in case there are multiple speakers and listeners waiting to be paired
		while(speakCount > 0 && listenCount > 0) {
			speakEmpty.wake();
			//manipulate word variable here?
			message = word;
			synchMessage.sleep();
			//put back to sleep
			System.out.println(KThread.currentThread().getName() + " " + "speaker put to sleep again");
			speakEmpty.sleep();
			//wake up listener if any
			listenEmpty.wakeAll();
			//return here speak will always return before listen?
			//return;
		}
		lock.release();
		return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		//int that will hold the message to return
		int retVal = 0;
		//acquire the lock
		lock.acquire();
		//if one of the condition variable queus is empty, sleep 
		
		//increment the counter for the waitqueue
		listenCount++;
		
		while(speakCount == 0 || listenCount == 0) {
			System.out.println(KThread.currentThread().getName() + " " + "listener put to sleep");
			listenEmpty.sleep();
		}
		
		while(speakCount > 0 && listenCount > 0) {
			listenEmpty.wake();
			retVal = message;
			//wake up speaker if any
			speakCount--;
			listenCount--;
			speakEmpty.wakeAll();
			
		}
		lock.release();
		return retVal;
	}
	
	//provided test cast from piazza
	public static void selfTest(){
	    final Communicator com = new Communicator();
	    final long times[] = new long[2];
	    final int words[] = new int[1];
	    
	    /* og code
	    * final long times[] = new long[4];
	    final int words[] = new int[2]; */
	    
	    KThread speaker1 = new KThread( new Runnable () {
	        public void run() {
	            com.speak(4);
	            times[0] = Machine.timer().getTime();
	    	    System.out.print("speak return");
	        }
	    });
	    speaker1.setName("S1");
	  /* og code
	   *  KThread speaker2 = new KThread( new Runnable () {
	        public void run() {
	            com.speak(7);
	            times[1] = Machine.timer().getTime();
	        }
	    });
	    speaker2.setName("S2");*/
	    KThread listener1 = new KThread( new Runnable () {
	        public void run() {
	            times[1] = Machine.timer().getTime();
	        	//times[2] = Machine.timer().getTime();
	            words[0] = com.listen();
	        }
	    });
	    listener1.setName("L1");
	    /* og code
	     * KThread listener2 = new KThread( new Runnable () {
	        public void run() {
	            times[3] = Machine.timer().getTime();
	            words[1] = com.listen();
	        }
	    });
	    listener2.setName("L2");*/
		    
	    speaker1.fork();
	    //speaker2.fork();
	    listener1.fork();
	    //listener2.fork();
	    
	    speaker1.join();
	    //speaker2.join();
	    listener1.join();
	    //listener2.join();
	    	    
	    Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word.");
	    Lib.assertTrue(times[0] > times[1], "speak() returned before listen() called.");

	    /* og code */
	    //Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word."); 
	    //Lib.assertTrue(words[1] == 7, "Didn't listen back spoken word.");
	    //Lib.assertTrue(times[0] > times[2], "speak() returned before listen() called.");
	    //Lib.assertTrue(times[1] > times[3], "speak() returned before listen() called.");
	    System.out.print("PASS");

	}
	
	//speakCount and listenCount are used to keep track of number of threads on the respective wait queues
	private int speakCount = 0;
	private int listenCount = 0;
	//message is the int that will be transferred between a speaker and listener
	private int message;
	//single lock to provide mutual exclusion
	private Lock lock = new Lock();
	//condition variables for speak and listen
	private Condition speakEmpty = new Condition(lock);
	private Condition listenEmpty = new Condition(lock);
	private Condition synchMessage = new Condition(lock);
	//discussion section said there should be a third condition variable... to check if there is a message to send?
}
