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
		this.lock = new Lock();
		this.speaker = new Condition(lock);
		this.listener = new Condition(lock);
		this.transmit = new Condition(lock);
		this.sent = false;
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
		
		while(sent) {
			speaker.sleep();
		}
		this.message = word;
		this.sent = true;
		
		listener.wake();
		transmit.sleep();
		
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		int word;
		lock.acquire();
		
		while(!sent) {
			listener.sleep();
		}
		word = this.message;
		this.sent = false;
		
		speaker.wake();
		transmit.wake();
		
		lock.release();
		return word;
	}
	
	//provided test cast from piazza
	public static void selfTest(){
	    final Communicator com = new Communicator();
	   
	    final long times[] = new long[6];
	    final int words[] = new int[3]; 
	    
	    KThread speaker1 = new KThread( new Runnable () {
	        public void run() {
	            com.speak(4);
	            times[0] = Machine.timer().getTime();
	    	    System.out.println("speak return");
	        }
	    });
	    speaker1.setName("S1");
	 
	    KThread speaker2 = new KThread( new Runnable () {
	        public void run() {
	            com.speak(7);
	            times[1] = Machine.timer().getTime();
	        }
	    });
	    speaker2.setName("S2");
	    
	    //added 3rd speaker
	    KThread speaker3 = new KThread( new Runnable () {
	        public void run() {
	            com.speak(10);
	            times[2] = Machine.timer().getTime();
	    	    System.out.println("speak return");
	        }
	    });
	    speaker3.setName("S3");
	    //end of 3rd speaker
	    
	    KThread listener1 = new KThread( new Runnable () {
	        public void run() {
	        	times[3] = Machine.timer().getTime();
	            words[0] = com.listen();
	        }
	    });
	    listener1.setName("L1");
	    
	    KThread listener2 = new KThread( new Runnable () {
	        public void run() {
	            times[4] = Machine.timer().getTime();
	            words[1] = com.listen();
	        }
	    });
	    listener2.setName("L3");
	    
	    //added 3rd listener
	    KThread listener3 = new KThread( new Runnable () {
	        public void run() {
	        	times[5] = Machine.timer().getTime();
	            words[2] = com.listen();
	        }
	    });
	    listener1.setName("L3");
	    //end 3rd listener
		    
	    speaker1.fork();
	    speaker2.fork();
	    speaker3.fork();
	    listener1.fork();
	    listener2.fork();
	    listener3.fork();
	    
	    speaker1.join();
	    speaker2.join();
	    speaker3.join();
	    listener1.join();
	    listener2.join();
	    listener3.join();
	    
	    //Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word.");
	    //Lib.assertTrue(times[0] > times[1], "speak() returned before listen() called.");
	    
	    Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word."); 
	    Lib.assertTrue(words[1] == 7, "Didn't listen back spoken word.");
	    Lib.assertTrue(words[2] == 10, "Didn't listen back spoken word.");
	    
	    Lib.assertTrue(times[0] > times[3], "speak() returned before listen() called.");
	    Lib.assertTrue(times[1] > times[4], "speak() returned before listen() called.");
	    Lib.assertTrue(times[2] > times[5], "speak() returned before listen() called" );
	    System.out.print("PASS");

	}
	
	private int message;
	private boolean sent;
	private Condition speaker;
	private Condition listener;
	private Condition transmit;
	private Lock lock;
}
