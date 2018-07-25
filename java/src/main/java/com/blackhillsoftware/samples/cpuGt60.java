package com.blackhillsoftware.samples;
                                                               
import java.io.IOException;                                                                     
import java.time.*;

import com.blackhillsoftware.smf.SmfRecord;                                                     
import com.blackhillsoftware.smf.SmfRecordReader;                                               
import com.blackhillsoftware.smf.smf30.ProcessorAccountingSection;                              
import com.blackhillsoftware.smf.smf30.Smf30Record;                                             
                                                                                                
public class cpuGt60                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                                                                                           
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromName(args[0])) 
        { 
            reader.include(30, 5);
            for (SmfRecord record : reader)                                                     
            {                                                                                   
                Smf30Record r30 = new Smf30Record(record);                                      
                for (ProcessorAccountingSection procAcct 
                        : r30.processorAccountingSections())   
                {                                                                               
                    Duration cpuTime = procAcct.smf30cpt()
                            .plus(procAcct.smf30cps());           
                    if (cpuTime.getSeconds() >= 60)                                             
                    {                                                                           
                        System.out.format("%-23s %-8s %12s%n",                                  
                           r30.smfDateTime(), 
                           r30.identificationSection().smf30jbn(), 
                           cpuTime);  
                    }                                                                           
                }                                                                               
            }                                                                                   
        }
        System.out.println("Done");
    }                                                                                           
}                                                                                               