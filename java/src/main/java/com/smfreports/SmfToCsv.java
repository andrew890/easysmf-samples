package com.smfreports;

import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class SmfToCsv                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                                       
        // If we received no arguments, open DD INPUT
        // otherwise use the first argument as the file 
        // name to read.
    	
        try (SmfRecordReader reader = 
                args.length == 0 ?
                        SmfRecordReader.fromDD("INPUT") :
                        SmfRecordReader.fromName(args[0])) 
        { 
        	System.out.format("%s,%s,%s,%s,%s,%s,%s,%n",
                    "Time", 
                    "System",
                    "Job",
                    "Program",
                    "CP Time",
                	"zIIP Time",
                	"zIIP on CP"
                	);
            reader.include(30,4)
                .stream()
                .map(record -> new Smf30Record(record)) 
                .filter(r30 -> r30.processorAccountingSection() != null) // 
                .filter(r30 -> r30.processorAccountingSection().smf30TimeZiipOnCpSeconds() > 0)
                .limit(1000)
                .forEach(r30 -> 
                    System.out.format("%s,%s,%s,%s,%.2f,%.2f,%.2f,%n",                                  
                        r30.smfDateTime(), 
                        r30.system(),
                        r30.identificationSection().smf30jbn(),
                        r30.identificationSection().smf30pgm(),
                        r30.processorAccountingSection().smf30cptSeconds()
                    		+ r30.processorAccountingSection().smf30cpsSeconds(),
                    	r30.processorAccountingSection().smf30TimeOnZiipSeconds(),
                    	r30.processorAccountingSection().smf30TimeZiipOnCpSeconds()
                    	));                                                                                 
        }
        System.out.println("Done");
    }                                                                                           
}                                                                                               