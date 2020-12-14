package com.smfreports.db2ddf;

import java.io.IOException;
import java.time.*;
import java.util.*;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.db2.Smf101Record;
import com.blackhillsoftware.smf.db2.section.*;

public class ssidCorr
{   
    public static void main(String[] args) throws IOException
    {
        try (SmfRecordReader reader 
        		= SmfRecordReader.fromName(args[0])
    				.include(101))
        {   
        	Map<String,    
        		Map<String, 
                Map<String, 
                Map<LocalDate, statistics>>>> stats = new HashMap<>();
        	
            for (SmfRecord r : reader)
            {
                Smf101Record r101 = new Smf101Record(r);   
                Qwhc qwhc = r101.qwhc();
                
                if (r101.ifcid() == 3
                    && (qwhc.qwhcatyp() == QwhcConnectionType.QWHCDUW 
                        || qwhc.qwhcatyp() == QwhcConnectionType.QWHCRUW
                        || !r101.qlac().isEmpty())
                    && !r101.qmda().isEmpty())
                {
                	String ptyp = r101.qmda().get(0).qmdaptyp();
                	
            		if ((ptyp.equals("SQL") || ptyp.equals("DSN") || ptyp.equals("JCL"))
            		        && r101.qlac().get(0).qlacsqls() == 0)
            		{
		                stats.computeIfAbsent(r101.system(), system -> new HashMap<>())
		            		.computeIfAbsent(r101.sm101ssi(), ssi -> new HashMap<>())
                            .computeIfAbsent(getCorrid(r101), corrid -> new HashMap<>())
                            .computeIfAbsent(r101.smfDateTime().toLocalDate(), date -> new statistics())
		                	.add(r101);		
                	}              	
                }
            }
            
            stats.entrySet().stream()
            	.sorted(Map.Entry.comparingByKey())
            	.forEachOrdered(systemEntry ->
            			systemEntry.getValue().entrySet().stream()
            				.sorted(Map.Entry.comparingByKey())
            				.forEachOrdered(ssiEntry ->
            				ssiEntry.getValue().entrySet().stream()
	            				.sorted(Map.Entry.comparingByKey())
	            				.forEachOrdered(corridEntry ->
    	            				corridEntry.getValue().entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .forEachOrdered(minuteEntry ->
                						{
                							System.out.format("%s %s %s %s %6d %6d %6d %8.3f %8.3f %8.3f %8.3f%8.3f%n", 
                									systemEntry.getKey(), 
                									ssiEntry.getKey(), 
                                                    corridEntry.getKey(), 
                                                    minuteEntry.getKey(), 
                                                    minuteEntry.getValue().count,
                                                    minuteEntry.getValue().commits,   
                                                    minuteEntry.getValue().aborts,   
                                                    minuteEntry.getValue().c1Tcb,
                                                    minuteEntry.getValue().c1Ziip,
                                                    minuteEntry.getValue().c2Tcb,
                                                    minuteEntry.getValue().c2Ziip,
                                                    minuteEntry.getValue().nonZiip);   
                						}))));
             
        }
    }

	private static String getCorrid(Smf101Record r101) 
	{
		String corrid = r101.qwhc().qwhccv();
		if (corrid.startsWith("ENTR") || corrid.startsWith("POOL"))
		{
			corrid = "CICS Tx " + corrid.substring(4, 8);
		}
		return corrid;
	}
    
    static class statistics
    {
    	void add(Smf101Record r101)
    	{
    	    count++;
    	    
    	    Qwac qwac = r101.qwac().get(0);
    		commits += qwac.qwaccomm();
    		aborts += qwac.qwacabrt();
    			 		
    		c1Tcb +=
    		        qwac.qwacejstSeconds()
    		        + qwac.qwacspcpSeconds()
    		        + qwac.qwacudcpSeconds();
    		
    		if (!qwac.qwacparr())
    		{
    		    c1Tcb -= qwac.qwacbjstSeconds();
    		}
    		
    		c1Ziip += qwac.qwaccls1ZiipSeconds();
    		
    		c2Tcb += qwac.qwacajstSeconds()
    		        + qwac.qwacspttSeconds()
    		        + qwac.qwacudttSeconds();

            c2Ziip += qwac.qwaccls2ZiipSeconds();

            nonZiip += qwac.qwacajstSeconds();
    	}
    	
        long count = 0;
        long commits = 0;
        long aborts = 0;
        
        double c1Tcb = 0;
        double c1Ziip = 0;
        
        double c2Tcb = 0;
        double c2Ziip = 0;
        
        double nonZiip = 0;    

        
        }    
}
