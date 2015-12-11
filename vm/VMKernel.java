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
		
		// Initialize size of inverted Page table to physical memory
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
  public static int allocate(int vpn, VMProcess proc) {
    //initial value for ppn... use -1 to indicate pin?
    int ppn = -1;

    //make sure there are free pages
    if(freePages.size() > 0) {
      	ppn = ((Integer)freePages.removeFirst()).intValue();
    }
    else{
    	//clock algorithm for victim 
	int toEvict = 0;

	memInfo mi  = null; 	
	System.out.println("ipt: " + invertedPageTable[clockhand]); 

	while(invertedPageTable[clockhand].te.used == true){	
		//TODO check pinned if pinned
		invertedPageTable[clockhand].te.used = false;
		clockhand = (clockhand+1) % (invertedPageTable.length);
	}
	toEvict = clockhand;
	clockhand = (clockhand+1) % (invertedPageTable.length); 
     
	TranslationEntry victim = invertedPageTable[toEvict].te;
	// Sync tlb entries
	TranslationEntry [] pageTable = invertedPageTable[toEvict].getPageTable();
	pageTable[victim.vpn].dirty = victim.dirty;
	pageTable[victim.vpn].used = victim.used;	
     	//if dirty swap out
     	if(victim.dirty){
		swapOut(toEvict, vpn);
	}
   
   //invalidate pte and tlb entry of victim
   invertedPageTable[victim.ppn] = null;
   pageTable[victim.vpn] = null;
}
   return ppn;
 }
	
	/*ClutchAF made 
 	 * Swapping from physical memory to disk
 	 * toSwap - invertedPageTable index of the evicted page?
 	 * */
	public static void swapOut(int toSwap, int vpn){
		// toSwap is index from physical memory that we want to evict
		memInfo info = invertedPageTable[toSwap];

		// Have to get swap page associated to toSwap
		Integer writeSpn = info.owner.vpnSpnPair.get(info.te.vpn);
 
		//if no mapping exists, increase swapFile and associate vpn
		if(writeSpn == null){
			writeSpn = lastSwapPage++; 
			info.owner.vpnSpnPair.put(vpn,writeSpn);
		}

		if(info.te.readOnly == true){
			return;
		}
		else{
			byte [] memory = Machine.processor().getMemory();
			if(info.te.dirty == true){
				swapFile.write(writeSpn*pageSize, memory, info.te.ppn*pageSize, pageSize); 
			}
			TranslationEntry toInvalidate = info.te;
			toInvalidate.valid = false; 
			//TODO ask why is this here.. lol
			info.owner.vpnSpnPair.remove(info.te.vpn);
		} 
	}
	
	// STEVEN
	// Swap page from disk to physical memory
	public static void swapIn(int vpn, VMProcess process){//, int ppn, VMProcess process){
		System.out.println("SwapIn");
		//memInfo info = invertedPageTable[ppn];
		memInfo mi = new memInfo(vpn, process); 
		byte [] memory = Machine.processor().getMemory();
		
		//HashMap<Integer, CoffSection> coffMap = info.getCoffMap();
		HashMap<Integer, CoffSection> coffMap = process.getCoffMap(); 

		Integer readSpn = mi.owner.vpnSpnPair.get(vpn);
		TranslationEntry [] pageTable = mi.getPageTable();

		if(readSpn != null){
			swapFile.read(readSpn*pageSize, memory, mi.te.ppn*pageSize,pageSize);//ppn*pageSize, pageSize);
				
		}/*else{
			CoffSection section = coffMap.get(vpn);
			 int offset = mi.te.vpn - section.getFirstVPN();
			//section.loadPage(readSpn, ppn);
			section.loadPage(offset, mi.te.ppn); 
		}*/
		/*if(coffMap.get(ppn) == null){
			System.out.println("Coff map is null");
			//zero out the whole page
     	 		byte[] buffer = new byte[pageSize];
			info.te.dirty = true;
      			System.arraycopy(buffer, 0, memory, info.te.ppn*pageSize, pageSize);
			
		}
		else{
			System.out.println("No page loading");
			CoffSection csection = coffMap.get(ppn);
			int offset = ppn - csection.getFirstVPN();
			csection.loadPage(offset, info.te.ppn);
		}*/
		TranslationEntry tlbEntry = pageTable[vpn];
		tlbEntry.valid = true;
		invertedPageTable[mi.te.ppn] = mi; // new memInfo(vpn, process);
	}

	/*ClutchAF made */
	//perhaps put PID, TranslationEntry, pinned ?
	public static class memInfo{
		int vpn;
		VMProcess owner;
		TranslationEntry te; 
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
		//STEVEN	
		public HashMap<Integer, CoffSection> getCoffMap(){
			return owner.getCoffMap();
		}
		//public TranslationEntry getEntry() { 
		//	return owner.getPageTable()[vpn];
		//}

	}
 
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
	
	/* ClutchAF variables*/
	private static int lastSwapPage = 0; 
	
	private static int clockhand = 0;

	public static OpenFile swapFile; 

	public String nameOfSwap = "clutchaf";

	public LinkedList<Integer> swapList;

	private static final int pageSize = Processor.pageSize;

	public int swapCounter = 0;

	public static memInfo [] invertedPageTable;

}
