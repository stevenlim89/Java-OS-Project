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
		vpnSpnPair = new HashMap<Integer, Integer>();
		coffMap = new HashMap<Integer, CoffSection>();
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

	public int pinVirtualPage(int vpn, boolean beingWritten){	
	
		if (vpn < 0 || vpn >= pageTable.length)
	    		return -1;
		TranslationEntry entry = pageTable[vpn];
		
		if (!entry.valid || entry.vpn != vpn)
	    		handlePageFault(vpn);

		if (beingWritten) {
	    		if (entry.readOnly)
				return -1;
	   		 entry.dirty = true;
		}
		VMKernel.invertedPageTable[entry.ppn].pinned = true;
		
		VMKernel.pinCounter++;

		entry.used = true;
	
		return entry.ppn;
	}

	public void unpinVirtualPage(int vpn){
		VMKernel.pinLock.acquire(); 
		
		TranslationEntry entry = pageTable[vpn];
		
		VMKernel.pinCounter--;

		VMKernel.invertedPageTable[entry.ppn].pinned = false;
		
		VMKernel.pinCond.wake();
		
		VMKernel.pinLock.release();
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
		VMKernel.bigLock.acquire(); 
		if(vaddr < 0 || vaddr >= numPages*pageSize){
			super.handleExit(0); 
		}
		
		Processor processor = Machine.processor();
	
		int vpn = processor.pageFromAddress(vaddr);

		TranslationEntry pte = pageTable[vpn];

    		if(!pte.valid) {
      			handlePageFault(vpn);
    		}

		int tlbSize = processor.getTLBSize();

		int evictIndex = 0;

		for(int i = 0; i < tlbSize; i++){
			TranslationEntry entry = processor.readTLBEntry(i);
			
			if(!entry.valid){
				evictIndex = i;
				break;
			}
			
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

		processor.writeTLBEntry(evictIndex, pte);
		VMKernel.bigLock.release();
	}

  public void handlePageFault(int vpn) {
    	TranslationEntry pte = pageTable[vpn];
	int ppn = VMKernel.allocate(vpn, this);
	//if it's a stack/args page
	if(coffMap.get(vpn) == null) {
	
		//new stack page	
		if( pte.ppn == -1)
		{
			byte[] buffer = new byte[pageSize];
			byte[] memory = Machine.processor().getMemory();
			System.arraycopy(buffer, 0, memory, ppn*pageSize, pageSize);
			pte.dirty = true;
		}
		else //load stack page from swap
		{
			VMKernel.swapIn(pte.vpn, this, ppn);
		}
    	}
	//if loading from coff
    	else if(vpnSpnPair.get(vpn) == null){
      		if(pte.readOnly == false){
			pte.dirty = true;
		}
		CoffSection csection = coffMap.get(vpn);
      		int offset = vpn - csection.getFirstVPN();
      		csection.loadPage(offset, ppn);

    	}
	//oading coff page from swap
	else{
		VMKernel.swapIn(pte.vpn,this,ppn);
	}	
	//set entry to true
    	pte.valid = true;
	pte.ppn = ppn;
 	VMKernel.memInfo info = new VMKernel.memInfo(vpn,this, false); 	
	VMKernel.invertedPageTable[pte.ppn] = info; 
	
  }
	
	/* ClutchAF made */
	public void syncTLBPTE( TranslationEntry te ){
		if(te.valid){
			pageTable[te.vpn].dirty = te.dirty;
			pageTable[te.vpn].used = te.used;
		}
	}

	/* ClutchAF made */
	public TranslationEntry[] getPageTable() {
		return pageTable;
	}	
	
	public HashMap<Integer, CoffSection> getCoffMap(){
		return coffMap;
	}
	
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
 	
	/* ClutchAF made , maps vpn to spn */	
	public HashMap<Integer, Integer> vpnSpnPair;
 	
	/* ClutchAF made*/
	private HashMap<Integer, CoffSection> coffMap;
}
