package com.smfreports.genericjobsummary;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;

public class StepsByProgramName {
	
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: StepsByProgramName <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))                
        {      	
        	JobGroupReport.runReport(
        			"Program Name", 
        			x -> x.identificationSection().smf30pgm(), // program name 
        			reader, 
        			new int[] { 4 }); // Subtype 4 Step End
        }
    }
}
