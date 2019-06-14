package com.blackhillsoftware.samples;
                                                               
import java.io.FileInputStream;
import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf15.Smf15Record;
                                  
public class StreamSmfSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                                                                                           
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0])))                
        { 
            long found = reader.include(15)
            	.stream()
            	.map(record -> new Smf15Record(record))
            	.filter(r15 -> r15.smfjfcb1().jfcbdsnm().equals("SYS1.PARMLIB"))
            	.map(r15 -> 
            	{
            		System.out.format("%s %s %s%n",                                  
                       r15.smfDateTime(), 
                       r15.system(),
                       r15.smf14jbn());
            		return r15;
            	})
            	.limit(1000)
            	.count();
            System.out.format("Found: %d", found);
        }
    }                                                                                           
}                                                                                               