package com.blackhillsoftware.samples;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0]));
             SmfRecordWriter writer = 
                args.length == 0 ?
                SmfRecordWriter.fromDD("OUTPUT") :
                SmfRecordWriter.fromStream(new FileOutputStream(args[1])); 
            )                
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
