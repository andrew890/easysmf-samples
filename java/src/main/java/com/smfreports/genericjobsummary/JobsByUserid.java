package com.smfreports.genericjobsummary;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;

public class JobsByUserid {
	
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: JobsByUserid <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))                
        {       	
        	JobGroupReport.runReport(
        			"UserID", 
        			x -> x.identificationSection().smf30rud(), // userid
        			reader, 
        			new int[] { 5 }); // Subtype 5 Job End
        }
    }
}
