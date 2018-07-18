//******************************************************
//  Memory Management Simulation Model 
// *****************************************************
//
//
// Import Packages
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import javax.management.*;
import cern.jet.random.engine.*;
import cern.jet.random.*;
import abcmod.evschedsimul.*;
//
// Some enumeration types
enum Kaction { ACCESSING, MEMACCESS, PAGEFAULT, IDLE };  // Addressing actions possible by kernel 
enum PagingAlgorithm { FIFO, LRU, CLOCK, COUNT };  // For definining paging algorithm used.

// The Simulation model Class
class MemManage extends EvSched  // Event Scheduling Simulation program
{
	// Constants (time units is microseconds
	final double FaultTime = 100;  // Time for faults 100 time units
	final double MemTime = 1;	// Time for acessing memory 1 time unit
	final int NumFrames = 32; 	// number of physical frames
	final int MeanMemAccesses = 20;  // mean number of memory accesses per process execution cycle
	/* Parameter */
        // Paging algorithm set in the Kernel Class
	
	/* Data Modules for implementing timing maps */
	Poisson memoryAccesses;  // for defining number of memory accesses by a process

	/* Resource Entity - the kernel */
	Kernel kernel;

	/* Aggregate Entities */
	ConcurrentLinkedQueue <Process> readyQueue;   	// process ready queue
	HashSet <Process> ioQueue;    // IO queue implemented as a set (to make code clearer)

	/* Output Sets */
	ESOutputSet phiTimeBtwFaults;   // Sample Set
	int numMemAccesses;  // Number of memory accesses with no page faults
	long numPer1000;

	// Methods
	// Model Behaviour
	// Constructor - Also nitialises the model
	public MemManage(PagingAlgorithm pgAlg, double t0time, double tftime, Seeds sd)
	{
		Process prc;
		int pid = 100;

		// Set up distribution functions
		memoryAccesses = new Poisson(MeanMemAccesses,  
				             new MersenneTwister(sd.mAcc));

		// System Initialization
		readyQueue = new ConcurrentLinkedQueue<Process>();
		ioQueue = new HashSet<Process>();
		// Lets do processes
                prc = new Process(pid++, 10,8,6,6,250,sd.prc1,sd.maprc1,sd.bernprc1);  // 30 pages
		kernel = new Kernel(NumFrames, pgAlg);
		kernel.processExecuting = prc;  // Set up first process as executing
		prc.numMemAccess = memoryAccesses.nextInt();
		kernel.processExecuting.vpage = selectAddress(prc);
		kernel.action=Kaction.PAGEFAULT;
		// Other processes added to ready queue
                prc = new Process(pid++, 10,6,4,4,150,sd.prc2,sd.maprc2,sd.bernprc2); // 24 pages
		readyQueue.add(prc);
                prc = new Process(pid++, 18,8,6,4,100,sd.prc3,sd.maprc3,sd.bernprc3);  // 36 pages
		readyQueue.add(prc);
                prc = new Process(pid++, 12,8,6,6,300,sd.prc4,sd.maprc4,sd.bernprc4); // 32 pages
		readyQueue.add(prc);
		
		// Setup sample set
		phiTimeBtwFaults = new ESOutputSet("phiTimeBtwFaults");
		numMemAccesses = 0;

		// Initialize the simulation model
		initEvSched(t0time,tftime);   

		preConditions();   // check preconditions to schedule event (first page fault)
	}
	
	// Future Events
	// Future Event Names (used in FEL)
	final int EndPageFault=1; // End of page fault
	final int EndMemoryAccess=2; // End of memory access

        // Translates event id to name(for loggin)
        public String getEventName(int num) 
	{
		switch(num)
		{
		  case EndPageFault: return("EndMemoryAccess");
		  case EndMemoryAccess: return("EndMemoryAccess");
		}
		return("Unknown");
	}
	
	public void processEvent(int fEvent, Object obj)
	{
		switch(fEvent)
		{
		  case EndPageFault: endPageFault((Process)obj); break;
		  case EndMemoryAccess: endMemoryAccess(); break;
	          default: System.out.println("Bad future event" + fEvent); break;
		}
	}

	private void endPageFault(Process cProcess)	// finished a page fault operation
	{
	   kernel.timeLastFault = clock;
	   KernelFunctions.pageReplacement(cProcess.vpage,cProcess,kernel);
	   if(!ioQueue.remove(cProcess)) System.out.println("Process "+cProcess.pid+" not in ioQueue");
	   else readyQueue.add(cProcess);
	   preConditions(); // start activities
	}

	private void endMemoryAccess()	// endMemoryAcess SCS 
	{
	   // PageFault and AccessMemory Terminating Event SCS 
	   KernelFunctions.doneMemAccess(kernel.processExecuting.vpage,kernel.processExecuting,clock); // flag last memory access
           if(kernel.processExecuting.numMemAccess <= 0)
	   {
	       readyQueue.add(kernel.processExecuting);  // adding current process to ready queue
	       kernel.processExecuting = (Process) readyQueue.poll();
	       if(kernel.processExecuting == null)
	       {
	           System.out.println("Error - no process on ready queue");
	       }
	       kernel.processExecuting.numMemAccess = memoryAccesses.nextInt();
	   }
           if(kernel.processExecuting.numMA2ChangeWS <= 0) kernel.processExecuting.updateWS();
	   else kernel.processExecuting.numMA2ChangeWS--;
	   if(kernel.processExecuting.vpageFromFault != -1)
	   {
	      kernel.processExecuting.vpage = kernel.processExecuting.vpageFromFault;
	      kernel.processExecuting.vpageFromFault = -1;
	   }
	   else kernel.processExecuting.vpage = selectAddress(kernel.processExecuting);
	   kernel.action = nextAction(kernel.processExecuting.vpage, kernel.processExecuting);
	   preConditions(); // start activities
	}

	// Check for starting events 
	private void preConditions()
	{
		while(true)  // loop until no preconditions are true
		{
			if(kernel.action == Kaction.PAGEFAULT)
			{
				phiTimeBtwFaults.put(clock,clock-kernel.timeLastFault);
				Process cProcess = kernel.processExecuting;
				cProcess.vpageFromFault = cProcess.vpage;  // flag page that caused fault
				ioQueue.add(cProcess);
	                        kernel.processExecuting = (Process) readyQueue.poll(); // Next process
	                        if(kernel.processExecuting == null)
				    kernel.action = Kaction.IDLE; // no processes to execute
				else
				{  // Execute the next process
	                           kernel.processExecuting.numMemAccess = memoryAccesses.nextInt();
	   			   if(kernel.processExecuting.vpageFromFault != -1)
				   {
					   kernel.processExecuting.vpage = kernel.processExecuting.vpageFromFault;
					   kernel.processExecuting.vpageFromFault = -1;
				   }
				   else kernel.processExecuting.vpage = selectAddress(kernel.processExecuting);
	   			   kernel.action = nextAction(kernel.processExecuting.vpage, kernel.processExecuting);
				}
				addEventNotice(EndPageFault,clock+FaultTime,cProcess);
			}
			else if(kernel.action == Kaction.MEMACCESS)
			{
				kernel.action = Kaction.ACCESSING;
				kernel.processExecuting.numMemAccess--;
				numMemAccesses++;
				addEventNotice(EndMemoryAccess,clock+MemTime);
			}
			else if((kernel.action == Kaction.IDLE) && (readyQueue.size() !=0) ) // StartExecuting Action
			{
	                    kernel.processExecuting = (Process) readyQueue.poll(); // Next process
	                    kernel.processExecuting.numMemAccess = memoryAccesses.nextInt();
	   		    if(kernel.processExecuting.vpageFromFault != -1)
			    {
			       kernel.processExecuting.vpage = kernel.processExecuting.vpageFromFault;
			       kernel.processExecuting.vpageFromFault = -1;
			    }
			    else kernel.processExecuting.vpage = selectAddress(kernel.processExecuting);
	   		    kernel.action = nextAction(kernel.processExecuting.vpage, kernel.processExecuting);
			}
                        else break;
		}
	}

	public boolean implicitStopCondition( )  // termination explicit
	{ return(false);}
	
	/************  Implementation User defined Modules ***********/
	public int selectAddress(Process prc)  // returns virtual page number being addressed
	{ return prc.selectAddressPrc(); } // defined as part of the class 

	public Kaction nextAction(int vpage,Process prc)
	{
	    if(prc.pageTable[vpage].valid) return(Kaction.MEMACCESS);
	    else return(Kaction.PAGEFAULT);
	}

	public void computeOutput()
	{
          phiTimeBtwFaults.computePhiDSOVs();
	  numPer1000 = (phiTimeBtwFaults.number*1000)/(numMemAccesses+phiTimeBtwFaults.number);
	}
}


// Defining the Consumer entities - the Process
class Process
{
	//-----------------------------------------
	//  General process data structures
	int pid;		// Process ID
	public int numPages;   // Number of virtual pages
	public PgTblEntry [] pageTable;   // Page table
	public int [] workingSet;   // List of virtual pages in working set
	int numAllocatedFrames;  // Number of allocated physical frames
	int [] allocatedFrames;     // List of allocated frames - contains frame numbers
	int framePtr;     // index into allocatedFrames.
	//-----------------------------------------
	// for Simulation of process execution to create locality of reference
	Poisson prcMA2ChangeWS;   // for getting a discrete random number
	Uniform prcDiscreteRandom;   // for getting a discrete random number
	Binomial prcBern;	 // To create Bernouilli random number generator
	int numMemAccess;	// number of memory accesses during a cycle executing
	int numMA2ChangeWS;     // number of memory accesses to change working set
	public int [] wsSegments;  // defines indexes in workingSet to divide into segments (code, data, etc.)
	public int vpage;     // virtual page being accessed
	public int vpageFromFault;     // virtual page just loaded after default - need to be accessed first executed.
	// Mumber of pages per segment of program
	int numCodePages;   // Code segment
	int numDataPages;   // Data segment
	int numStackPages;   // Stack segment
	int numHeapPages;   // Heap segment

	public Process(int pid, int nCode, int nData, int nStack, int nHeap, int meanMA, int sd1, int sd2, int sd3)
	{
	   int i;  // an index
	   // Setup random number generators
	   prcDiscreteRandom = new Uniform(new MersenneTwister(sd1));
	   prcMA2ChangeWS = new Poisson(meanMA,new MersenneTwister(sd2));
	   prcBern = new Binomial(1,0.90,new MersenneTwister(sd3));
           // Setup process numbers
	   this.pid = pid;
	   numPages = nCode+nData+nStack+nHeap;
	   numCodePages=nCode;
	   numDataPages=nData;
	   numStackPages=nStack;
	   numHeapPages=nHeap;
	   // Setup the page table entry - all invalid
	   pageTable = new PgTblEntry[numPages];
	   for(i = 0 ; i<numPages ; i++) 
	   {
	     pageTable[i] = new PgTblEntry();
	     pageTable[i].valid = false;
	     pageTable[i].used = false;
	     pageTable[i].tmStamp = 0;
	   }
	   allocateFrames();  // Allocates frames - current fixed allocation 
	   // Setup working set and list to generate page references
	   wsSegments = new int[7];  // 0 - end of code 1 - end of data 2 - end of stack 3 - end of heap
	   			     // 4 - next section to address.
				     // 5,6 - pages to create locality of reference
	   updateWS();        // Sets up the working set
	}

	public void updateWS()
	{
	    int ncode, ndata, nstack, nheap;  // number of pages in each segment
	    int ix;  // index into working set
	    int i;

	    // Radomly select number of pages from each segment
	    ncode = prcDiscreteRandom.nextIntFromTo(1, (numCodePages/2));
	    ndata = prcDiscreteRandom.nextIntFromTo(1, (numDataPages/2));
	    nstack = prcDiscreteRandom.nextIntFromTo(1, (numStackPages/2));
	    nheap = prcDiscreteRandom.nextIntFromTo(1, (numHeapPages/2));
	    if(ncode == 0) ncode = 1;
	    if(ndata == 0) ndata = 1;
	    if(nstack == 0) nstack = 1;
	    if(nheap == 0) nheap = 1;

	    workingSet = new int[ncode+ndata+nstack+nheap];
	    ix = 0;
	    for(i=0 ; i<ncode ; i++)  // Code pages
	    { workingSet[ix] = getPageNumforWS(0,numCodePages-1,workingSet,ix); ix ++; }
	    wsSegments[0] = ix-1;  // defines how many code pages are in working set
	    for(i=0 ; i<ndata ; i++) // Data pages
	    { workingSet[ix] = getPageNumforWS(numCodePages,numCodePages+numDataPages-1,workingSet,ix); ix ++; }
	    wsSegments[1] = ix-1;  // defines how many data pages are in working set
	    for(i=0 ; i<nstack ; i++) // Stack pages
	    { workingSet[ix] = getPageNumforWS(numCodePages+numDataPages,
				              numCodePages+numDataPages+numStackPages-1,
					      workingSet,ix); 
	      ix ++; }
	    wsSegments[2] = ix-1;  // defines how many stack pages are in working set
	    for(i=0 ; i<nheap ; i++)  // Heap pages
	    { workingSet[ix] = getPageNumforWS(numCodePages+numDataPages+numStackPages,
				              numCodePages+numDataPages+numStackPages+numHeapPages-1,
					      workingSet,ix); 
	      ix ++; }
	    wsSegments[3] = ix-1;  // defines how many heap pages are in working set
	    wsSegments[4] = 0;  // start with a code page
	    numMA2ChangeWS = prcMA2ChangeWS.nextInt();  // Number of references before changing working set
	}

	private int getPageNumforWS(int start, int end, int [] list, int len)
	{
	    int num;
	    while(true)
	    { // Keep asking for number until one not in list found
		    // This can take a while if numbers in list contains
		    // most of the numbers being selected AND an
		    // infinite loop if all numbers are in the list
	        num = prcDiscreteRandom.nextIntFromTo(start,end);
	        if(notInIntArray(num,list,len)) return(num);
	    }
	}

	private boolean notInIntArray(int i, int [] array, int len)
        {
	   int ix;
	   for(ix = 0 ; ix < len ; ix++)
	   { if(array[ix] == i) return(false); }
	   return(true);
        }

	// Address is selected from code, data, stack and heap sections of the working
	// set - this to improve locality of reference
	public int selectAddressPrc()
	{
	    int start;
	    int end;
	    int num;
	    int pageSelected;
		// Define start index and end index of segment into Working set
	    // Skip if 0 or 1 - code or data segments
	    if((areAllocatedFramesFull()) && (prcBern.nextInt() == 1)) 
	    {  // Select from the allocated pages
		num = prcDiscreteRandom.nextIntFromTo(5,6);  // using code and other page
		pageSelected = wsSegments[num];  // reusing pages
	    }
	    else
	    {// Select from the working set
	        if(wsSegments[4] == 0) start = 0;  // Start
	        else start = wsSegments[wsSegments[4]-1]+1;
	        end = wsSegments[wsSegments[4]];  // End

	        // Randomly select a page from the segment in working set
	        if( (end-start)==0)  // only one page - no need to select
	        {
	            pageSelected = workingSet[end];
	        }
	        else
	        {
	            num = prcDiscreteRandom.nextIntFromTo(start,end);
		    pageSelected = workingSet[num];
	        }
		if(wsSegments[4] == 0)  // code segment
			wsSegments[5] = pageSelected;
		else		// other segment
			wsSegments[6] = pageSelected;
	        wsSegments[4] = (wsSegments[4]+1)%4;
	    }
            return(pageSelected);
	}

	public boolean areAllocatedFramesFull()
        {
	    if(allocatedFrames == null) return(false);
	    if(allocatedFrames.length < numAllocatedFrames) return(false);
	    return(true);

        }

	//******************************************************************
	//                 Methods for supporting allocation schemes
	//******************************************************************
	public void allocateFrames()
	{
	    numAllocatedFrames = 5;
	    allocatedFrames = null;  // no frames allocated yet
	    framePtr = 0;  // point to the first entry - not used until allocateFrames is filled
	    	               // This is to support FIFO replacement algorithm
	}
}


// Defining the Resource entity - the kernel
class Kernel
{
	public int numFrames;
	public int [] freeList;
	public Process processExecuting;	// executing process
	public Kaction action ;				// Action to be taken - set to:
					//  ACCESSING - in the middle of accessing memory
					//  MEMACESS - next action is to access memory no page fault
					//  PAGEFAULT - next action is to access memory with page fault
	double timeLastFault;		// time of last page fault.
	PagingAlgorithm pagingAlgorithm;

	public Kernel(int numFrames, PagingAlgorithm pgAlg)
	{
	   int i;
	   pagingAlgorithm = pgAlg;
	   timeLastFault = 0;
	   action = Kaction.PAGEFAULT;
	   this.numFrames = numFrames;
	   freeList = new int [numFrames];
	   for(i=0 ; i<numFrames ; i++) freeList[i] = i;
	}

	public int getNextFreeFrame()
	{
            int [] fl;
	    int freeFrame;
	    int i;

	    if(freeList == null) return(-1); // list is empty return -1
	    freeFrame = freeList[0]; // gets next free frame
	    // Update the freeList (removes the head)
	    fl = new int[freeList.length-1];
	    if(freeList.length == 1) freeList = null;
	    else
	    {
	       for(i=0 ; i<freeList.length-1 ; i++) fl[i] = freeList[i+1];
	       freeList = fl;
	    }
	    // Return free frame
	    return(freeFrame);
	}

	public void returnFrame2FreeList(int fr)
	{
 	    int [] fl;  // to create new free list
	    int i;

	    // Update the freeList (removes the head)
	    if(freeList == null)
	    {
	       freeList = new int[1];
	       freeList[0] = fr;
	    }
	    else
	    {
	       fl = new int[freeList.length+1]; // create new list and copy current list
	       for(i=0 ; i<freeList.length; i++) fl[i] = freeList[i];
	       fl[i] = fr;  // appends newly released frame
	       freeList = fl;
	    }
	}

}

class Seeds
{
	int mAcc;   // for number of memory accesses
	int dRand;   // kernel random number generator
	int prc1;   // process 1 random number generator
	int prc2;   // process 2 random number generator
	int prc3;   // process 3 random number generator
	int prc4;   // process 4 random number generator
	int maprc1;   // process 1 random number generator
	int maprc2;   // process 2 random number generator
	int maprc3;   // process 3 random number generator
	int maprc4;   // process 4 random number generator
	int bernprc1;   // process 1 random number generator
	int bernprc2;   // process 2 random number generator
	int bernprc3;   // process 3 random number generator
	int bernprc4;   // process 4 random number generator

	public Seeds(int sd1,int sd2, int sd3, int sd4, int sd5, int sd6,
	             int sd7,int sd8, int sd9, int sd10, int sd11, int sd12,
		     int sd13, int sd14)
	{
	    mAcc=sd1;
	    dRand=sd2;
	    prc1=sd3;
	    prc2=sd4;
	    prc3=sd5;
	    prc4=sd6;
	    maprc1=sd7;
	    maprc2=sd8;
	    maprc3=sd9;
	    maprc4=sd10;
	    bernprc1=sd11;
	    bernprc2=sd12;
	    bernprc3=sd13;
	    bernprc4=sd14;
	}
}
