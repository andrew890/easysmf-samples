package com.smfreports.genericjobsummary;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;

public class JobsByJobClass {
	
    public static void main(String[] args) throws IOException
    {
        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {    	       	
        	JobGroupReport.runReport(
        			"Job Class", 
        			x -> x.identificationSection().smf30cl8(), // job class
        			reader,
        			new int[] { 5 }); // Subtype 5 Job End
        }
    }
}
