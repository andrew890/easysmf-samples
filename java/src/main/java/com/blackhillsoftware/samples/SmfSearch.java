package com.blackhillsoftware.samples;

import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf15.Smf15Record;

public class SmfSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                                
        String searchString = "SYS1.PARMLIB";
        
        try (SmfRecordReader reader = 
                args.length == 0 ?
                        SmfRecordReader.fromDD("INPUT") :
                        SmfRecordReader.fromName(args[0])) 
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