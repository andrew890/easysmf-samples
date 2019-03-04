package com.blackhillsoftware.samples;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class Type30Filter {
	
	/**
	 * 
	 * Sample to demonstrate filtering type 30 data, extracting fields and 
	 * writing output in CSV or human readable form.
	 * 
	 * @param args optional name of file containing SMF data, if not present INPUT DD will be used
	 * @throws IOException
	 */
	
    public static void main(String[] args) throws IOException                                   
    {              
        // If we received no arguments, open DD INPUT otherwise use the argument as a
        // file name.
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromName(args[0]))                
        { 
        	// Human readable output
        	String headingFormat = "%-24s %-6s %-10s %-10s %8s %8s %10s%n";
        	String outputFormat = "%-24s %-6s %-10s %-10s %8.2f %8.2f %10d%n";

        	// Alternative: CSV format
        	//String headingFormat = "%s,%s,%s,%s,%s,%s,%s%n";
        	//String outputFormat = "%s,%s,%s,%s,%f,%f,%d%n";
        	
        	System.out.format(headingFormat, 
        			"Time", "System", "Jobname", "Program", "CP", "zIIP", "EXCP");
        	
            reader
                .include(30,4)
            	.stream()
            	.map(record -> new Smf30Record(record))
            	.filter(r30 -> 
            		r30.processorAccountingSection() != null
            		&& r30.identificationSection().smf30pgm().equals("JVMLDM80"))
            	.forEach(r30 -> 
            		System.out.format(outputFormat,                                  
                       r30.smfDateTime(), 
                       r30.system(),
                       r30.identificationSection().smf30jbn(),
                       r30.identificationSection().smf30pgm(),
                       r30.processorAccountingSection().smf30cptSeconds()
                       		+ r30.processorAccountingSection().smf30cpsSeconds()
                       		+ r30.processorAccountingSection().smf30icuSeconds()
                       		+ r30.processorAccountingSection().smf30isbSeconds()
                       		+ r30.processorAccountingSection().smf30iipSeconds()
                       		+ r30.processorAccountingSection().smf30rctSeconds()
                       		+ r30.processorAccountingSection().smf30hptSeconds(),
                       r30.processorAccountingSection().smf30TimeOnZiipSeconds(),
                       r30.ioActivitySection().smf30tex()));
            System.out.println("Finished");
        }
    }   
}
