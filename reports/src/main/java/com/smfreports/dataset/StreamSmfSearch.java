package com.smfreports.dataset;
                                                               
import java.io.IOException;                                                                     
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf15.Smf15Record;
                                  
public class StreamSmfSearch                                                                            
{                                                                                               
    public static void main(String[] args) throws IOException                                   
    {                 
        if (args.length < 1)
        {
            System.out.println("Usage: StreamSmfSearch <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))                
        { 
            long found = reader.include(15)
            	.stream()
            	.map(record -> Smf15Record.from(record))
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