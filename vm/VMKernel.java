package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

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

		swapList = new LinkedList<Integer>();
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
    else{
     //sync

     //clock algorithm for victim 
     	int victimIndex = 0; 
	int toEvict = 0;

	//evict the page with victimIndex of zero 
	while(invertedPageTable[victimIndex].getEntry().used == true){
		//memInfo[victimIndex].getEntry.useBit = 0;
		invertedPageTable[victimIndex].getEntry().used = false;
		victimIndex = (victimIndex+1) % (invertedPageTable.length);
	}
	toEvict = victimIndex;
	victimIndex = (victimIndex+1) % (invertedPageTable.length); 
     
	TranslationEntry victim = invertedPageTable[toEvict].getEntry();
		
     	//if dirty swap out
     	if(victim.dirty){
		//swapOut(victimIndex);
	}

     //invalidate pte and tlb entry of victim 
    }

    return ppn;
  }
	
	/*ClutchAF made 
 	 * Swapping from disk to physical memory
 	 * 
 	 * */
	public void swapOut(int toSwap){
		memInfo info = invertedPageTable[toSwap];
		
		Integer spn = info.owner.vpnSpnPair.get(info.vpn);
		if(spn == null){
			if(swapList.isEmpty()){
				swapCounter++;
				swapList.add(swapCounter);
			} 

			spn = swapList.removeFirst();
		}
	}

	/*ClutchAF made */
	public static class memInfo{
		int vpn;
		VMProcess owner; 
		//pinned for later
		
		public memInfo(int vpn, VMProcess owner){
			this.vpn = vpn;
			this.owner = owner;
		}
		
		public TranslationEntry[] getPageTable() {
			return owner.getPageTable();
		}
		public TranslationEntry getEntry() { 
			return owner.getPageTable()[vpn];
		}

	}
 
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
	
	/* ClutchAF variables*/
	int handOfClock = 0;
	
	public LinkedList<Integer> swapList;

	public int swapCounter = 0;

	public static memInfo [] invertedPageTable = new memInfo[Machine.processor().getNumPhysPages()]; 

}
