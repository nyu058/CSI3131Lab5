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
class CountExp
{
   public static void main(String[] args)
   {
       double startTime=0.0, endTime=10000;  // .25 second
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
	
       // Loop for NUMRUN simulation runs
       System.out.println("Running simulation");
       mmng = new MemManage(PagingAlgorithm.COUNT,startTime,endTime,sds);
       mmng.runSimulation();
       mmng.computeOutput();
       System.out.println("Number of faults: "+mmng.phiTimeBtwFaults.number);
       System.out.println("Number memory accesses (no faults): "+mmng.numMemAccesses);
       System.out.println("Number of faults per 1000 references: "+mmng.numPer1000);
   }
}
