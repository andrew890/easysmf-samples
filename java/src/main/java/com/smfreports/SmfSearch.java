package com.smfreports;

import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf15.Smf15Record;

/**
 * Search SMF type 15 (Output Dataset Activity) for a specific dataset name.
 * Prints the time, system and jobname of jobs that opened the dataset for output. 
 */

public class SmfSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                                       
        if (args.length < 1 || args.length > 2)
        {
            System.out.println("Usage: SmfSearch DATASET.NAME [input-file]");
            System.out.println("If input-file is omitted DD INPUT will be used.");          
            return;
        }
        
        String searchString = args[0];

        // The first argument is the dataset name
        // If we received only 1 argument, open DD INPUT
        // otherwise use the second argument as the file 
        // name to search.        
        try (SmfRecordReader reader = 
                args.length == 1 ?
                        SmfRecordReader.fromDD("INPUT") :
                        SmfRecordReader.fromName(args[1])) 
        { 
            reader.include(15)
                .stream()
                .map(record -> new Smf15Record(record))
                .filter(r15 -> r15.smfjfcb1().jfcbdsnm().equals(searchString))
                .limit(1000)
                .forEach(r15 -> 
                    System.out.format("%s %s %s%n",                                  
                        r15.smfDateTime(), 
                        r15.system(),
                        r15.smf14jbn()));                                                                                 
        }
        System.out.println("Done");
    }                                                                                           
}                                                                                               