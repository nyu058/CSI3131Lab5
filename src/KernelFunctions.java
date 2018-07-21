

public class KernelFunctions {


	// ******************************************************************
	// Methods for supporting page replacement
	// ******************************************************************

	// the following method calls a page replacement method once
	// all allocated frames have been used up
	// DO NOT CHANGE this method
	public static void pageReplacement(int vpage, Process prc, Kernel krn) {
		if (prc.pageTable[vpage].valid)
			return; // no need to replace

		if (!prc.areAllocatedFramesFull()) // room to get frames in allocated list
			addPageFrame(vpage, prc, krn);
		else
			pageReplAlgorithm(vpage, prc, krn);
	}

	// This method will all a page frame to the list of allocated
	// frames for the process and load it with the virtual page
	// DO NOT CHANGE this method
	public static void addPageFrame(int vpage, Process prc, Kernel krn) {
		int[] fl; // new frame list
		int freeFrame; // a frame from the free list
		int i;
		// Get a free frame and update the allocated frame list
		freeFrame = krn.getNextFreeFrame(); // gets next free frame
		if (freeFrame == -1) // list must be empty - print error message and return
		{
			System.out.println("Could not get a free frame");
			return;
		}
		if (prc.allocatedFrames == null) // No frames allocated yet
		{
			prc.allocatedFrames = new int[1];
			prc.allocatedFrames[0] = freeFrame;
		} else // some allocated but can get more
		{
			fl = new int[prc.allocatedFrames.length + 1];
			for (i = 0; i < prc.allocatedFrames.length; i++)
				fl[i] = prc.allocatedFrames[i];
			fl[i] = freeFrame; // adds free frame to the free list
			prc.allocatedFrames = fl; // keep new list
		}
		// update Page Table
		prc.pageTable[vpage].frameNum = freeFrame;
		prc.pageTable[vpage].valid = true;
	}

	// Calls to Replacement algorithm
	public static void pageReplAlgorithm(int vpage, Process prc, Kernel krn) {
		switch (krn.pagingAlgorithm) {
		case FIFO:
			pageReplAlgorithmFIFO(vpage, prc);
			break;
		case LRU:
			pageReplAlgorithmLRU(vpage, prc);
			break;
		case CLOCK:
			pageReplAlgorithmCLOCK(vpage, prc);
			break;
		case COUNT:
			pageReplAlgorithmCOUNT(vpage, prc);
			break;
		}
	}

	// --------------------------------------------------------------
	// The following methods need modification to implement
	// the three page replacement algorithms
	// ------------------------------------------------------------
	// The following method is called each time an access to memory
	// is made (including after a page fault). It will allow you
	// to update the page table entries for supporting various
	// page replacement algorithms.
	public static void doneMemAccess(int vpage, Process prc, double clock) {

		if (prc.pageTable[vpage].valid) {
			prc.pageTable[vpage].used = true;
			prc.pageTable[vpage].tmStamp = clock;
			//prc.pageTable[vpage].count=0;
		}
	}

	// FIFO page Replacement algorithm
	public static void pageReplAlgorithmFIFO(int vpage, Process prc) {
		int vPageReplaced; // Page to be replaced
		int frame; // frame to receive new page
		// Find page to be replaced
		// logProcessState(prc);
		frame = prc.allocatedFrames[prc.framePtr]; // get next available frame
		vPageReplaced = findvPage(prc.pageTable, frame); // find current page using it (i.e written to disk)
		prc.pageTable[vPageReplaced].valid = false; // Old page is replaced.
		prc.pageTable[vpage].frameNum = frame; // load page into the frame and update table
		prc.pageTable[vpage].valid = true; // make the page valid
		prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length; // point to next frame in list
	}

	// finds the virtual page loaded in the specified frame fr
	public static int findvPage(PgTblEntry[] ptbl, int fr) {
		int i;
		for (i = 0; i < ptbl.length; i++) {
			if (ptbl[i].valid) {
				if (ptbl[i].frameNum == fr) {
					return (i);
				}
			}
		}
		System.out.println("Could not find frame number in Page Table " + fr);
		return (-1);
	}

	// CLOCK page Replacement algorithm
	public static void pageReplAlgorithmCLOCK(int vpage, Process prc) {
		int vPageReplaced; // Page to be replaced
		int frame; // frame to receive new page

		while (true) {
			frame = prc.allocatedFrames[prc.framePtr];
			vPageReplaced = findvPage(prc.pageTable, frame);
			if (prc.pageTable[vPageReplaced].used == true) {// if the page is referenced, give it a second change by
															// flipping the bit
				prc.pageTable[vPageReplaced].used = false;

			} else {
				prc.pageTable[vPageReplaced].valid = false; // Old page is replaced.
				prc.pageTable[vpage].frameNum = frame; // load page into the frame and update table
				prc.pageTable[vpage].valid = true;// make the page valid
				break; // stop iteration if a page is replaced
			}
			prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length; // increment the circular list pointer
		}
	}

	// LRU page Replacement algorithm

	public static void pageReplAlgorithmLRU(int vpage, Process prc) {
		int lruIndex = 0;

		for (int i = 0; i < prc.pageTable.length; i++) {// find first valid index that has a time stamp to start
			if (prc.pageTable[i].valid) {
				lruIndex = i;
				break; // stop iteration if an entry is valid
			}
		}
		for (int j = 0; j < prc.pageTable.length; j++) {
			if (prc.pageTable[j].valid && prc.pageTable[j].tmStamp < prc.pageTable[lruIndex].tmStamp) {// find the entry
																										// with the
																										// lowest time
																										// stamp
				lruIndex = j;
			}
		}
		prc.pageTable[lruIndex].valid = false; // Old page is replaced.
		prc.pageTable[vpage].frameNum = prc.pageTable[lruIndex].frameNum; // replace lru page with the new page
		prc.pageTable[vpage].valid = true; // make the page valid


	}

	public static void pageReplAlgorithmCOUNT(int vpage, Process prc) {
		int vPageReplaced; // Page to be replaced
		int frame; // frame to receive new page
		int iter; // page table iterator
		frame = prc.allocatedFrames[prc.framePtr];
		vPageReplaced = findvPage(prc.pageTable, frame);
		int i= (prc.framePtr+1)%prc.allocatedFrames.length;
		while (i != prc.framePtr) {
			frame = prc.allocatedFrames[i]; 
			iter = findvPage(prc.pageTable, frame); // find current frame in page table
			if (prc.pageTable[iter].count < prc.pageTable[vPageReplaced].count) {
				vPageReplaced = iter;
			}
			i=(i+1)%prc.allocatedFrames.length;
		}
		prc.pageTable[vPageReplaced].valid = false; // Old page is replaced.
		prc.pageTable[vPageReplaced].count = 0;
		prc.pageTable[vpage].frameNum = prc.pageTable[vPageReplaced].frameNum; // load page into the frame and update table
		prc.pageTable[vpage].valid = true; // make the page valid
		prc.pageTable[vpage].count = 0;
		prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;// point to next frame in list

	}

	// *******************************************
	// The following method is provided for debugging purposes
	// Call it to display the various data structures defined
	// for the process so that you may examine the effect
	// of your page replacement algorithm on the state of the
	// process.
	// Method for displaying the state of a process
	// *******************************************
	public static void logProcessState(Process prc) {
		int i;

		System.out.println("--------------Process " + prc.pid + "----------------");
		System.out
				.println("Virtual pages: Total: " + prc.numPages + " Code pages: " + prc.numCodePages + " Data pages: "
						+ prc.numDataPages + " Stack pages: " + prc.numStackPages + " Heap pages: " + prc.numHeapPages);
		System.out.println("Simulation data: numAccesses left in cycle: " + prc.numMemAccess
				+ " Num to next change in working set: " + prc.numMA2ChangeWS);
		System.out.println("Working set is :");
		for (i = 0; i < prc.workingSet.length; i++) {
			System.out.print(" " + prc.workingSet[i]);
		}
		System.out.println();
		// page Table
		System.out.println("Page Table");
		if (prc.pageTable != null) {
			for (i = 0; i < prc.pageTable.length; i++) {
				if (prc.pageTable[i].valid) // its valid printout the data
				{
					System.out.println("   Page " + i + "(valid): " + " Frame " + prc.pageTable[i].frameNum + " Used "
							+ prc.pageTable[i].used + " Time Stamp " + prc.pageTable[i].tmStamp + " Count "
							+ prc.pageTable[i].count);
				} else
					System.out.println("   Page " + i + " is invalid (i.e not loaded)");
			}
		}
		// allocated frames
		System.out.println("Allocated frames (max is " + prc.numAllocatedFrames + ")" + " (frame pointer is "
				+ prc.framePtr + ")");
		if (prc.allocatedFrames != null) {
			for (i = 0; i < prc.allocatedFrames.length; i++)
				System.out.print(" " + prc.allocatedFrames[i]);
		}
		System.out.println();
		System.out.println("---------------------------------------------");
	}
}

// Page Table Entries
class PgTblEntry {
	int frameNum; // Frame number
	boolean valid; // Valid Bit
	boolean used; // Used Bit
	double tmStamp; // Time Stamp
	int count;
}
