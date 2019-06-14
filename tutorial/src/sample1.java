import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class sample1 
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
        	// Write headings
        	System.out.format("%s,%s,%s,%s,%s,%s,%s%n",
                    "Time", 
                    "System",
                    "Job",
                    "Job Number",
                    "CP Time",
                	"zIIP Time",
                	"zIIP on CP"
                	);
        	
        	reader.include(30,5); // SMF type 30 subtype 5 : Job End records
        	
        	for (SmfRecord record : reader)
        	{
        		Smf30Record r30 = Smf30Record.from(record);
        		if (r30.processorAccountingSection() != null)
        		{
                    System.out.format("%s,%s,%s,%s,%.2f,%.2f,%.2f%n",                                  
                            r30.smfDateTime(), 
                            r30.system(),
                            r30.identificationSection().smf30jbn(),
                            r30.identificationSection().smf30jnm(),
                            r30.processorAccountingSection().smf30cptSeconds()
                        		+ r30.processorAccountingSection().smf30cpsSeconds(),
                        	r30.processorAccountingSection().smf30TimeOnZiipSeconds(),
                        	r30.processorAccountingSection().smf30TimeZiipOnCpSeconds()
                        	);
        		}
        	}                                                                           
        }
        System.out.println("Done");
    }

}
