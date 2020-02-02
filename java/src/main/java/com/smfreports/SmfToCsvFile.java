package com.smfreports;

import java.io.FileWriter;
import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class SmfToCsvFile                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                       	
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]);
             FileWriter writer = new FileWriter("C:\\Users\\Andrew\\Desktop\\output.csv")) 
        { 
        	writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%n",
                    "Time", 
                    "System",
                    "Job",
                    "Program",
                    "CP Time",
                	"zIIP Time",
                	"zIIP on CP"
                	));
            reader.include(30,4)
                .stream()
                .map(record -> new Smf30Record(record)) 
                .filter(r30 -> r30.processorAccountingSection() != null) // 
                .filter(r30 -> r30.processorAccountingSection().smf30TimeZiipOnCpSeconds() > 0)
                .filter(r30 -> r30.identificationSection().smf30pgm().equals("JVMLDM80"))
                .limit(1000)
                .forEach(r30 -> 
                    {
						try {
							writer.write(String.format("%s,%s,%s,%s,%.2f,%.2f,%.2f,%n",                                  
							    r30.smfDateTime(), 
							    r30.system(),
							    r30.identificationSection().smf30jbn(),
							    r30.identificationSection().smf30pgm(),
							    r30.processorAccountingSection().smf30cptSeconds()
									+ r30.processorAccountingSection().smf30cpsSeconds(),
								r30.processorAccountingSection().smf30TimeOnZiipSeconds(),
								r30.processorAccountingSection().smf30TimeZiipOnCpSeconds()
								));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});                                                                                 
        }
        System.out.println("Done");
    }                                                                                           
}                                                                                               