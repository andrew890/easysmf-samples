package com.blackhillsoftware.samples;
                                                               
import java.io.FileInputStream;
import java.io.IOException;                                                                     

import com.blackhillsoftware.smf.SmfRecord;                                                     
import com.blackhillsoftware.smf.SmfRecordReader;                                               
import com.blackhillsoftware.smf.db2.*;                                                                          
import com.blackhillsoftware.smf.db2.section.Qwhc;
                                                                                                
public class db2sample                                                                            
{                                                                                               
    static final String columns = "%-8s %-12s %-8s %-8s %-8s %8s %-44s %-16s %-32s %-18s %n";
    static final String headingCols =
    				"%n%n" +
    				columns + 
    				"%n";
    
    public static void main(String[] args) throws IOException                                   
    {                                                                                           
        int linesPerPage = 55;
        int linesOnPage = 0;
        
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0])))                
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