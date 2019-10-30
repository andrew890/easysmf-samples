package com.smfreports.genericjobsummary;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;

public class StepsByProgramName {
	
    public static void main(String[] args) throws IOException
    {
        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {    	       	
        	JobGroupReport.runReport(
        			"Program Name", 
        			x -> x.identificationSection().smf30pgm(), // program name 
        			reader, 
        			new int[] { 4 }); // Subtype 4 Step End
        }
    }
}
