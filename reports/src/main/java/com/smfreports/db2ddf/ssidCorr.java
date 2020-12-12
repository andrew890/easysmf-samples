package com.smfreports.db2ddf;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.db2.Smf101Record;
import com.blackhillsoftware.smf.db2.section.Qlac;
import com.blackhillsoftware.smf.db2.section.Qmda;

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
        			Map<String, statistics>>> stats = new HashMap<>();
        	
            for (SmfRecord r : reader)
            {
                Smf101Record r101 = new Smf101Record(r);
                
                if (r101.ifcid() == 3)
                {
                	Qlac qlac = r101.qlac().get(0);
                	Qmda qmda = r101.qmda().get(0);
                	String ptyp = qmda.qmdaptyp();
                	
            		if (qlac.qlacsqls() == 0
            			&& (ptyp.equals("SQL") || ptyp.equals("DSN") || ptyp.equals("JCL")))
            		{
		                stats.computeIfAbsent(r101.system(), system -> new HashMap<>())
		            		.computeIfAbsent(r101.sm101ssi(), ssi -> new HashMap<>())
		                	.computeIfAbsent(corrid(r101), corrid -> new statistics())
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

            						{
            							System.out.format("%s %s %s %d%n", 
            									systemEntry.getKey(), 
            									ssiEntry.getKey(), 
            									corridEntry.getKey(), 
            									corridEntry.getValue().count);   
            						})));
             
        }
    }

	private static String corrid(Smf101Record r101) 
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
    	}
    	
    	int count = 0;
    }
    
    
    
}
