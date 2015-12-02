package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

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
		return super.loadSections();
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
		Processor processor = Machine.processor();
	
		/** STEP 1 */	
		// get virtual page number
		int vpn = processor.pageFromAddress(vaddr);
		
		TranslationEntry inputEntry = null;

		// Get size of TLB
		int tlbSize = processor.getTLBSize();

		// use vpn to locate pte in page table
		TranslationEntry pte = pageTable[vpn];

		// Index in TLB to evict
		int evictIndex = 0;

		/** STEP 2 */
		// Loop through TLB to find invalid entry
		for(int i = 0; i < tlbSize; i++){
			TranslationEntry entry = processor.readTLBEntry(i);
			if(!entry.valid){
			// need to evict page somehow. I think writeTLBEntry overwrites it?
				evictIndex = i;
				break;
			}
			
			// if all of the entries are valid, evict random one
			if(i == tlbSize - 1){
				evictIndex = Lib.random(tlbSize);
				break;
			}
		}
		
		/** STEP 3 */
		processor.writeTLBEntry(evictIndex, pte);
	     //	}
	}
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
