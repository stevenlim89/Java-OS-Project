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
		
		//creates an OpenFile called swapFile 
		swapFile = ThreadedKernel.fileSystem.open(nameOfSwap, true);
		
		invertedPageTable = new memInfo[Machine.processor().getNumPhysPages()]; 
 
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
      ppn = ((Integer)freePages.removeFirst()).intValue();
    }
    else{

        //clock algorithm for victim 
     	int clockhand = 0; //physical page number 
	int toEvict = 0;

	//evict the page with victimIndex of zero 
	memInfo mi  = null; 
	
	for(int i = 0; i <  invertedPageTable.length; i++){
		//TODO check pinned if pinned
		 mi = invertedPageTable[i]; 
		 mi.getEntry().used = true;


		invertedPageTable[clockhand].getEntry().used = false;
		clockhand = (clockhand+1) % (invertedPageTable.length);
	}
	toEvict = clockhand;
	clockhand = (clockhand+1) % (invertedPageTable.length); 
     
	TranslationEntry victim = invertedPageTable[toEvict].getEntry();
		
     	//if dirty swap out
     	if(victim.dirty){
		//not clockhand right? should i put the RO, proc, and vpn as parameters??
		swapOut(toEvict, vpn);
	}
	//call swapIn	
	//TODO create load page method to handle putting swap or coff or new stack page in memoryarray
	//sync tlb
   
	  //invalidate pte and tlb entry of victim 
    }

    return ppn;
  }
	
	/*ClutchAF made 
 	 * Swapping from disk to physical memory
 	 * toSwap - invertedPageTable index of the evicted page?
 	 * */
	public void swapOut(int toSwap, int vpn){
		memInfo info = invertedPageTable[toSwap];

		Integer writeSpn = vpnSpnPair.get(info.getEntry.vpn); 
		//if no mapping exists, increase swapFile 
		if(writeSpn == null){
			writeSpn = lastSwapPage++; 
			vpnSpnPair.put(vpn,writeSpn);
		}

		if(info.getEntry().readOnly == true){
			return;
			//TODO put in load page as well
			//coffMap(get the coffsection to vpn)
			//section.loadPage(writeSpn,ppn); 
		}
		else{
			if(info.getEntry().dirty == true){
				swapFile.write(writeSpn*pageSize, memory, ppn*pageSize, pageSize); 
			}
			TranslationEntry toInvalidate =  info.getEntry();
			toInvalidate.valid = false; 
		}
	//TODO put in load page to set entry valid
	//	TranslationEntry entry = pageTable[vpn];
	//	entry.valid = true; 

	}

	/*ClutchAF made */
	//perhaps put PID, TranslationEntry, pinned ?
	public static class memInfo{
		int vpn;
		VMProcess owner; 
		//pinned for later
		int pinCount = 0;
	
		public memInfo(int vpn, VMProcess owner){// TranslationEntry te){
			this.vpn = vpn;
			this.owner = owner;
			this.te = owner.getPageTable()[vpn]; 
		}
		
		public TranslationEntry[] getPageTable() {
			return owner.getPageTable();
		}
		//public TranslationEntry getEntry() { 
		//	return owner.getPageTable()[vpn];
		//}

	}
 
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
	
	/* ClutchAF variables*/
	private int lastSwapPage = 0; 

	public OpenFile swapFile; 

	public String nameOfSwap = "clutchaf";

	public LinkedList<Integer> swapList;

	public int swapCounter = 0;

	public static memInfo [] invertedPageTable;

}
