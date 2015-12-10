package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
		
		Processor processor = Machine.processor();	
		int tlbSize = processor.getTLBSize();

		for(int i = 0; i < tlbSize; i++){
			TranslationEntry entry = processor.readTLBEntry(i);
			if(entry.valid){
				pageTable[entry.vpn] = entry;
				entry.valid = false;
				processor.writeTLBEntry(i, entry);
        entry.valid = true;
			}
		}
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		Processor processor = Machine.processor();

		if(processor.getNumPhysPages() < numPages){
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
    
    //allocate the page table
		pageTable = new TranslationEntry[numPages];

    //allocate the entries but do not initialize yet bc lazy loading
    //set ppn to -1 to indicate the need to initialize in handleTLBmiss
    for (int vpn=0; vpn<numPages; vpn++) {
      pageTable[vpn] = new TranslationEntry(vpn, -1, false, false, false, false);
    }
    
    for(int s = 0; s < coff.getNumSections(); s++){
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				
				TranslationEntry entry = pageTable[vpn];

				coffMap.put(vpn, section);

				entry.valid = false;
				entry.readOnly = section.isReadOnly();
			}
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss: handleTLBMiss(processor.readRegister(Processor.regBadVAddr)); break;
		default:
			super.handleException(cause);
			break;
		}
	}

	public void handleTLBMiss(int vaddr){
		
		//check validity of vaddr
		if(vaddr < 0 || vaddr >= numPages*pageSize){
			//what to put as the exit status? 0 or -1?
			super.handleExit(0); 
		}
		
		Processor processor = Machine.processor();
	
		// get vpn 
		int vpn = processor.pageFromAddress(vaddr);
		System.out.println("tlb miss vpn: " + vpn);

		// get pte from vpn
		TranslationEntry pte = pageTable[vpn];

		//IF INVALID = handlePageFault
    		if(!pte.valid) {
      			handlePageFault(vpn);
    		}

		//IF VALID, ALLOCATE TLB ENTRY
		// Get size of TLB
		int tlbSize = processor.getTLBSize();

    		// initialize index to tlb to evict page
		int evictIndex = 0;

		// Loop through TLB to find invalid entry
		for(int i = 0; i < tlbSize; i++){
			TranslationEntry entry = processor.readTLBEntry(i);
			
			//if invalid, grab index 
			if(!entry.valid){
				evictIndex = i;
				break;
			}
			
			// if all of the entries are valid, evict random one
			if(i == tlbSize - 1){
				evictIndex = Lib.random(tlbSize);
				entry = processor.readTLBEntry(evictIndex);
				
				// Synch victim's page table entry
				if(entry.valid){
					int victimVPN = entry.vpn;
					pageTable[victimVPN].dirty = entry.dirty;
					pageTable[victimVPN].used = entry.used;
				}
				break;
			}
		}

		
		//UPDATE TLB
		processor.writeTLBEntry(evictIndex, pte);
	     
	}

  public void handlePageFault(int vpn) {
    TranslationEntry pte = pageTable[vpn];

    //if first time initializing entry
    if(pte.ppn == -1) {
      pte.ppn = VMKernel.allocate(vpn, pte.readOnly, this); 
    }
	
    //if(pte.dirty == true){
//	VMKernel.swapIn(pte.vpn,this);
  //  }
    else{
    	//check if from stack/args bc vpn for coff will be in coffMap
    	if(coffMap.get(vpn) == null) {
      		//zero out the whole page
     	 	byte[] buffer = new byte[pageSize];
      		byte[] memory = Machine.processor().getMemory();
		pte.dirty = true;
      		System.arraycopy(buffer, 0, memory, pte.ppn*pageSize, pageSize);
    	}
    	//load from coff
    	else {
      	//get coffsection and offset to load page 
      		CoffSection csection = coffMap.get(vpn);
      		int offset = vpn - csection.getFirstVPN();
      		csection.loadPage(offset, pte.ppn);
    	}
    }
    //set entry to true
    pte.valid = true;
  }
	/* ClutchAF made */
	public TranslationEntry[] getPageTable() {
		return pageTable;
	}	
	
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
 	
	/* ClutchAF made , maps vpn to spn */	
	public HashMap<Integer, Integer> vpnSpnPair = new HashMap<Integer, Integer>();
 	
	/* ClutchAF made*/
	private HashMap<Integer, CoffSection> coffMap = new HashMap<Integer, CoffSection>();

}
