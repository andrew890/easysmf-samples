package com.blackhillsoftware.samples;

import java.io.IOException;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.db2.*;

public class Db2Select 
{
	public static void main(String[] args) throws IOException 
	{
        int in = 0;
        int out = 0;        
        try (SmfRecordReader reader = SmfRecordReader.fromDD("INPUT");
        	 SmfRecordWriter writer = SmfRecordWriter.fromDD("OUTPUT");)                
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
            			&& r100.qwhc().qwhcopid().equals("MYUSER"))
            		{
            			writer.write(r100);
            			out ++;
            		}
            		break;
            	case 101:
            		Smf101Record r101 = new Smf101Record(record);
            		if (r101.qwhc() != null 
            			&& r101.qwhc().qwhcopid().equals("MYUSER"))
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
