package com.smfreports.genericjobsummary;

import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecordReader;

public class IntervalByUserid {
	
    public static void main(String[] args) throws IOException
    {
        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
    	
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {    	       	
        	JobGroupReport.runReport(
        			"UserID", 
        			x -> x.identificationSection().smf30rud(), // userid
        			reader, 
        			new int[] { 2, 3 }); // 2 and 3 interval records
        }
    }
}
