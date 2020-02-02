package com.smfreports;

import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.SmfRecordWriter;
import com.blackhillsoftware.smf.db2.Smf100Record;

public class FilterDb2
{
    // filter db2 records - copy type 100 records with IFCID 2 and SM100SSI = DBS1
    // from input to output.
    
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("Usage: FilterDb2 <input-name> <output-name>");
            System.out.println("<input/output-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
    	
        // SmfRecordReader.fromName(...) and SmfRecordWriter.fromName(...) accept
        // a filename, a DD name in the format //DD:DDNAME or MVS dataset name 
        // in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]);
        	 SmfRecordWriter writer = SmfRecordWriter.fromName(args[1]);)                             
        {   
            int out = 0;
            reader.include(100);
            for (SmfRecord r : reader)
            {
                Smf100Record r100 = new Smf100Record(r);
                if (r100.ifcid() == 2 && r100.sm100ssi().equals("DBS1"))
                {
                    out++;
                    writer.write(r100);
                }                    
            }
            System.out.println("Finished, " + Integer.toString(out) + " records written.");
        }
    }
}
