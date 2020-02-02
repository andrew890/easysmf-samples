package com.smfreports;
                                                               
import java.io.IOException;                                                                     

import com.blackhillsoftware.smf.SmfRecord;                                                     
import com.blackhillsoftware.smf.SmfRecordReader;                                               
import com.blackhillsoftware.smf.db2.*;                                                                          
import com.blackhillsoftware.smf.db2.section.Qwhc;
                                                                                                
public class Db2Sample                                                                            
{                                                                                               
    static final String columns = "%-8s %-12s %-8s %-8s %-8s %8s %-44s %-16s %-32s %-18s %n";
    static final String headingCols =
    				"%n%n" +
    				columns + 
    				"%n";
    
    public static void main(String[] args) throws IOException                                   
    {
        if (args.length < 1)
        {
            System.out.println("Usage: Db2Sample <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        int linesPerPage = 55;
        int linesOnPage = 0;
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))                
        { 
            reader.include(101);
            writeHeadings();
            for (SmfRecord record : reader)                                                     
            {
                Smf101Record r101 = new Smf101Record(record);   
            	Qwhc qwhc = r101.qwhc();
                if (qwhc  != null)
                {
	            	if (linesOnPage >= linesPerPage) {
	            		writeHeadings();
	            		linesOnPage = 0;
	            	}                
	                System.out.format(columns,                                  
	                		qwhc.qwhcaid(),
	                		qwhc.qwhccv(), 
	                		qwhc.qwhccn(), 
	                		qwhc.qwhcplan(), 
	                		qwhc.qwhcopid(), 
	                		qwhc.qwhcatyp(), 
	                		qwhc.qwhctokn(), 
	                		qwhc.qwhceuid(), 
	                		qwhc.qwhceutx(), 
	                		qwhc.qwhceuwn());
	                linesOnPage++;
                }
            }                                                                                   
        }                                                                                       
    }

	private static void writeHeadings() {
		System.out.format(headingCols,                                  
		    "QWHCAID", 
		    "QWHCCV",
		    "QWHCCN", 
		    "QWHCPLAN",
		    "QWHCOPID",
		    "QWHCATYP",
		    "QWHCTOKN",
		    "QWHCEUID",
		    "QWHCEUTX",
		    "QWHCEUWN");
	}                                                                                           
}                                                                                               