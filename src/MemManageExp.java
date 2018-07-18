// File: MemManageExp.java
// Description:
//    Simulation of memory management system

// Import packages
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import cern.jet.random.engine.*;
import cern.jet.random.*;
import abcmod.evschedsimul.*;

// Main Method: Experiments
// 
class MemManageExp
{
   public static void main(String[] args)
   {
       double startTime=0.0, endTime=5000000;  // 5 seconds
       Seeds sds;
       MemManage mmng; 

       // Lets get a set of uncorrelated seeds
       RandomSeedGenerator rsg = new RandomSeedGenerator();
       sds = new Seeds( rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed(),
		       	rsg.nextSeed(), rsg.nextSeed());
	
       // Use FIFO page replacement algorithm
       System.out.println("Running simulation using FIFO");
       mmng = new MemManage(PagingAlgorithm.FIFO,startTime,endTime,sds);
       mmng.runSimulation();
       mmng.computeOutput();
       System.out.println("Number of faults: "+mmng.phiTimeBtwFaults.number);
       System.out.println("Number memory accesses (no faults): "+mmng.numMemAccesses);
       System.out.println("Number of faults per 1000 references: "+mmng.numPer1000);
       System.out.println();

       // Use CLOCK page replacement algorithm
       System.out.println("Running simulation using CLOCK");
       mmng = new MemManage(PagingAlgorithm.CLOCK,startTime,endTime,sds);
       mmng.runSimulation();
       mmng.computeOutput();
       System.out.println("Number of faults: "+mmng.phiTimeBtwFaults.number);
       System.out.println("Number memory accesses (no faults): "+mmng.numMemAccesses);
       System.out.println("Number of faults per 1000 references: "+mmng.numPer1000);
       System.out.println();

       // Use LRU page replacement algorithm
       System.out.println("Running simulation using LRU");
       mmng = new MemManage(PagingAlgorithm.LRU,startTime,endTime,sds);
       mmng.runSimulation();
       mmng.computeOutput();
       System.out.println("Number of faults: "+mmng.phiTimeBtwFaults.number);
       System.out.println("Number memory accesses (no faults): "+mmng.numMemAccesses);
       System.out.println("Number of faults per 1000 references: "+mmng.numPer1000);
       System.out.println();

       // Use Counting page replacement algorithm
       System.out.println("Running simulation using COUNT");
       mmng = new MemManage(PagingAlgorithm.COUNT,startTime,endTime,sds);
       mmng.runSimulation();
       mmng.computeOutput();
       System.out.println("Number of faults: "+mmng.phiTimeBtwFaults.number);
       System.out.println("Number memory accesses (no faults): "+mmng.numMemAccesses);
       System.out.println("Number of faults per 1000 references: "+mmng.numPer1000);
       System.out.println();
   }
}
