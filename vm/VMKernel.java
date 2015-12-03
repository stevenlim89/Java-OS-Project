package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

  //method to lazy load the physical page for that vpn
  public static int allocate(int vpn, boolean RO, VMProcess proc) {
    //initial value for ppn... use -1 to indicate pin?
    int ppn = -1;

    //make sure there are free pages
    if(freePages.size() > 0) {
      //remove doesn't tell you if empty but pollFirst returns null if empty
      ppn = ((Integer)freePages.removeFirst()).intValue();
    }

    return ppn;
  }

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
}
