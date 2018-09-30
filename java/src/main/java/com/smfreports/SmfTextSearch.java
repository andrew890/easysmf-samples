package com.smfreports;

import java.io.IOException;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;

public class SmfTextSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {
        String searchString = "SYS1.PARMLIB";
        int dumpContextBytes = 64;
        int maxResults = 1000;
        
        try (SmfRecordReader reader = 
                args.length == 0 ?
                    SmfRecordReader.fromDD("INPUT") :
                    SmfRecordReader.fromName(args[0])) 
        { 
            int count = 0;

            for (SmfRecord record : reader)
            {
                if (record.recordType() != 14)
                {
                    int location = record.toString().indexOf(searchString);
                    if (location != -1)
                    {
                        count++;
                        // print record time, system, type and subtype
                        System.out.format("%-24s %s Type: %3d Subtype: %3s%n",                                  
                                record.smfDateTime(), 
                                record.system(),
                                record.recordType(),
                                record.hasSubtypes() ? 
                                        Integer.toString(record.subType()) : "");
                        // dump bytes surrounding found string
                        System.out.format("%n%s%n",
                                record.dump(
                                        Math.max(0, location - dumpContextBytes),
                                        Math.min(dumpContextBytes * 2, 
                                                record.recordLength() - location)));
                    }  		
                }
                if (count >= maxResults)
                {
                    break;
                }
            }

            System.out.format("Found: %d%n", count);                  
        }                                   
    }                                                                                           
}                                                                                               