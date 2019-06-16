import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecordReader;

public class sample3
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
	            .stream()
	            .filter(record -> record.recordType() != 14 && record.recordType() != 15)
	            .filter(record -> !(record.recordType() == 42 && record.subType() == 6))
	            .filter(record -> record.toString().contains("SYS1.PARMLIB"))
	            .limit(100)
	            .forEach(record -> 
	            {
	                System.out.format("%-23s System: %-4s Record Type: %s Subtype: %s%n%n",                                  
	                		record.smfDateTime(), 
	                		record.system(),
	                		record.recordType(),
	                		record.hasSubtypes() ? record.subType() : ""); 
	                System.out.format("%s%n%n",                                  
	                		record.dump());
	            });
        }
        System.out.println("Done");
    }

}
