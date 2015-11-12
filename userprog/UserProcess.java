package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;


/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		
		this.fdArray[0] = new FileDescriptor(UserKernel.console.openForReading());
		this.fdArray[1] = new FileDescriptor(UserKernel.console.openForWriting());	
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages >  Machine.processor().getNumPhysPages()){
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, vpn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallOpen:
			return handleOpen(a0);
		case syscallWrite:
			return handleWrite(a0,a1,a2);
		case syscallRead:
         		return handleRead(a0,a1,a2);
    		case syscallExit:
			System.out.println("enteirng exit");
			handleExit(a0);
			return 0;
	 	case syscallCreate:
			return handleCreate(a0);	
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);	
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	// Handler for open system call
	private int handleOpen(int address) {
		//get name of the file
		if(address == 0){
			return -1;
		}
		String name  = readVirtualMemoryString(address, maxLength);
		// check if file is in virtual memory
		if(name == null){
			return -1;
		}
		//find Filelink associated with name in hashmap
		FileLinks fl = filemap.get(name);
		//check if it is linked when the file is not null
		if(fl != null){
			if(fl.Unlink){
				return -1;
			}
		}
		//create an Openfile with the filename, not truncated
		OpenFile file = ThreadedKernel.fileSystem.open(name, false);
		// check if file exists in the file system
		if(file == null)
			return -1;

	 	int slot = findEmptySlot();	
		// check to see if the file descriptor array is full or not
		if(slot == -1)
			return -1;

			
		// if it is null, add to hashmap, wait is this necessary...
		if(fl == null){
			filemap.put(name, new FileLinks());
			//increment count? BUT WHAT IS COUNT?? lol 
		}	
				
		this.fdArray[slot] = new FileDescriptor(file);
		
		return slot;
	}
	
	public int handleCreate(int address) {
		//get name of the file
		if(address == 0){
			return -1;
		}
		String name = readVirtualMemoryString(address,maxLength);
		//check validity of name
		if(name==null){
			return -1;
		}
		//get the FileLink associated with name
		FileLinks fl = filemap.get(name);
		//check if the filelinked is unlinked, if it is, return -1	
		if(fl != null){
			if(fl.Unlink){
				return -1;
			}
		}
		//Create an openfile that is truncated (empty basically)
		OpenFile file = ThreadedKernel.fileSystem.open(name,true);
		//check file for any errors
		if(file==null){
			return -1;
		}
		
		//find an empty slot inside the fdArray
		int slot = findEmptySlot();
		//if there is an erro, return -1
		if(slot == -1){
			return -1;
		}
			//otherwise check that the fileLink is null and put it in the 
		//hasmap
		if(fl == null){
			filemap.put(name, new FileLinks());
		}
		this.fdArray[slot] = new FileDescriptor(file);
		//return where it is created
		return slot;
	}
	public void handleExit(int status){
     		System.out.println("I finished!");
		KThread.finish();
		return;
	}

	public int handleWrite( int fd, int bufptr, int length){		
		// length should be positive
		if(bufptr > Machine.processor().getNumPhysPages() * pageSize){
			return -1;
		}
		
		if(length < 0){
			System.out.println("length is negative :(");
			return -1;
		}
		
		//check if file descriptor is in bound (16)
		
		if(fd > numberOfFD || fd < 0){
                        System.out.println("fd is out of bounds");
			return -1;
		}
		
		//checks validity of fd slot and openfile 
		if(this.fdArray[fd] == null){
			System.out.println("slot is null");
			return -1;
		}
	 	if(this.fdArray[fd].file==null){
			System.out.println("file is null");
			return -1;
		}	
		//create local buffer
		byte buffer [] = new byte[length];
	
		//read process's VA and copy process's buffer into local buffer
		int bytesTransferred = readVirtualMemory(bufptr,buffer);
		
		if(bytesTransferred < 0){
			return -1;
		}
		//create Openfile object to write to and return the bytes written
		OpenFile of = this.fdArray[fd].file;
		
		int bytesWritten = of.write(buffer, 0,bytesTransferred);
		
		if(bytesWritten < 0){
			return -1;
		}
		
		return bytesWritten;
	}
	
	public int handleRead( int fd, int bufptr, int length){  
		// length should be positive
	
		if(length < 0){
			System.out.println("length is negative :(");
			return -1;
		}
		
		//check if file descriptor is in bound (16)
		if(fd > numberOfFD || fd < 0){
                        System.out.println("fd value = " + fd);
			return -1;
		}
		
		//checks validity of fd slot and openfile 
		if(this.fdArray[fd] == null){
			System.out.println("slot is null");
			return -1;
		}
	 	if(this.fdArray[fd].file==null){
			System.out.println("file is null");
			return -1;
		}	
		//create local buffer
		byte buffer [] = new byte[length];

		//read process's VA and copy process's buffer into local buffer		
		OpenFile of = this.fdArray[fd].file;
		 
		int bytesRead = of.read(buffer,0,length);
		
		//check to see if buffer is invalid
		if(bytesRead <0){
			System.out.println("My bytes are negative!");
			return -1;
		}
		//System.out.println("My write value is: " + writeVirtualMemory(bufptr,buffer));
		int written = writeVirtualMemory(bufptr,buffer);
	
		if(written < 0){
			return -1;
		}
		return written;

	}

	private int handleClose(int fd){
		//checks validity of fd (in bound, non negative)
		if(fd >= numberOfFD || fd < 0){
			return -1;
		}
		FileDescriptor fdObj = fdArray[fd];
		//checks if fd object is valid 
		if(fdObj == null){
			return -1;
		}		
		//close openFile and release associated resources whether
		//or not it is a file or a stream. must close both similarly 
		fdObj.file.close();
		//slot at fd is null so it can be reused since the file
		//associated with it is closed
		fdArray[fd] = null;
		//Get name of the filename
		String name = fdObj.file.getName();
		//Use filename to find the fileLink associated with the name
		//in the hashmap
		FileLinks fl = filemap.get(name);
		//if filelink is null, it is a stream
		if(fl == null){
			return 0;	
		}
		//otherwise fl is a file. you want to reduce the open counter
		//checks if it has been opened before?
		if(fl.opened == 1){
			//remove from the hashmap since we closed it
			if(name != null){
				filemap.remove(name);
				if(fl.Unlink){
					if(!UserKernel.fileSystem.remove(name))
						return -1;
				}
			}	
		}
		else{
			fl.opened -= 1;
		}
		return 0;
	}
	

	private int handleUnlink(int nameAddress){
		String name = readVirtualMemoryString(nameAddress, maxLength);

		if(name == null)
			return -1;

		FileLinks link = filemap.get(name);

		if(link == null){
			boolean remove = UserKernel.fileSystem.remove(name);
			if(!remove)
				return -1;
		}
		else{
			link.Unlink = true;
		}

		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** File Descriptor Class */
	private class FileDescriptor{
		public FileDescriptor(OpenFile f){
			this.file = f;
			
		}
		
		private OpenFile file = null;
	}
	
	/** FileLinks Class*/
	private class FileLinks{
		public FileLinks(){}
		private int opened = 1;
		private boolean Unlink = false;
	}
	
	
	
	/** Method to check for an empty slot in the file
	 * Descriptor Array.
	 * @return
	 */
	private int findEmptySlot(){
		int counter = 0;
		while(counter < numberOfFD){
			if(fdArray[counter] == null){
				return counter;
			}
			else{
				counter++;
			}
		}
		return -1;
	}
	
	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;
	
	/** Hashmap for pairing unlink boolean with file object */
	private static HashMap<String, FileLinks> filemap = new HashMap<String, FileLinks>();
	
	private FileDescriptor fdArray [] = new FileDescriptor [16];
	private static final int maxLength = 256;
	private static final int numberOfFD = 16;
	
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
}
