import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf14.Smf14Record;

public class sample2
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
            reader
	        	.include(14)            	
	        	.include(15)
	            .stream()
	            .map(record -> Smf14Record.from(record)) // Smf14Record also maps type 15
	            .filter(r14 -> r14.smfjfcb1().jfcbdsnm().equals("SYS1.PARMLIB"))
	            .limit(1000000)
	            .forEach(r14 -> 
	                System.out.format("%-23s %-4s %-8s %-6s%n",                                  
	                    r14.smfDateTime(), 
	                    r14.system(),
	                    r14.smf14jbn(),
	                    r14.smf14rty() == 14 ? "Read" : "Update"));                                                                           
        }
        System.out.println("Done");
    }

}
