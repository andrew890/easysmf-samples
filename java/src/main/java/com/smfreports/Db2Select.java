package com.smfreports;

import java.io.IOException;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.db2.*;

public class Db2Select 
{
	public static void main(String[] args) throws IOException 
	{
        if (args.length < 3)
        {
            System.out.println("Usage: Db2Select <input-name> <output-name> <qwhcopid-value>");
            System.out.println("<input/output-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
		
        int in = 0;
        int out = 0;
        
        // SmfRecordReader.fromName(...) and SmfRecordWriter.fromName(...) accept
        // a filename, a DD name in the format //DD:DDNAME or MVS dataset name 
        // in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]);
        	 SmfRecordWriter writer = SmfRecordWriter.fromName(args[1]);)                
        { 
            reader.include(100);
            reader.include(101);
            for (SmfRecord record : reader)                                                     
            {
            	in++;
            	switch (record.recordType())
            	{
            	case 100:
            		Smf100Record r100 = new Smf100Record(record);
            		if (r100.qwhc() != null 
            			&& r100.qwhc().qwhcopid().equals(args[2]))
            		{
            			writer.write(r100);
            			out ++;
            		}
            		break;
            	case 101:
            		Smf101Record r101 = new Smf101Record(record);
            		if (r101.qwhc() != null 
            			&& r101.qwhc().qwhcopid().equals(args[2]))
            		{
            			writer.write(r101);
            			out++;
            		}
            		break;
            	}                                              
            }                                                                                   
        }
        System.out.format("Records In: %d Records Out: %d %n", in, out);
	}
}
