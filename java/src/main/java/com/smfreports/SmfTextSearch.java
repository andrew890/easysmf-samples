package com.smfreports;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecordReader;

/**
 *
 * Search all SMF records for a text string, 
 * and print Time, System, Type and Subtype for 
 * each record found.
 *
 */
public class SmfTextSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {
        if (args.length < 1 || args.length > 2)
        {
            System.out.println("Usage: SmfTextSearch string [input-file]");
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
            reader
            .stream()
            .filter(record -> record.recordType() != 14) // Exclude type 14 (read dataset)
            .filter(record -> record.toString().contains(searchString))
            .limit(1000)
            .forEach(record -> 
                // print record time, system, type and subtype
                System.out.format("%-24s %s Type: %3d Subtype: %3s%n",                                  
                        record.smfDateTime(), 
                        record.system(),
                        record.recordType(),
                        record.hasSubtypes() ? 
                                Integer.toString(record.subType()) : ""));

            System.out.format("Done");                  
        }                                   
    }                                                                                           
}                                                                                               