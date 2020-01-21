package com.blackhillsoftware.samples;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf41.*;

public class Type41
{
    public static void main(String[] args) throws IOException
    {

    	List<VlfEntry> vlfStatistics = new ArrayList<VlfEntry>();

        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0])) 
        {       
            reader.include(41, 3);
            for (SmfRecord record : reader)
            {
                Smf41Record r41 = new Smf41Record(record);
                for (VlfStatisticsDataSection vlfSection : r41.vlfStatisticsDataSections())
                {
                    vlfStatistics.add(new VlfEntry(r41, vlfSection));                   
                }
            }
        }
        
        
        // write report
        vlfStatistics.stream()
        	.collect(Collectors.groupingBy(x -> x.system)) // group by system name
        	.entrySet().stream() // stream groups
        	.sorted((a, b) -> a.getKey().compareTo(b.getKey())) // process groups in key order (system)
        	.forEachOrdered(systemData -> // forEachOrdered guarantees to preserve current order
        	{
        	    // process entries from each system 
        	    // print a System ID heading 
        	    System.out.format("%n%nSystem: %s%n", systemData.getKey());
            		
        	    systemData.getValue().stream() // stream entries
        	        .collect(Collectors.groupingBy(x -> x.smf41cls)) // group again by vlf class
                	.entrySet().stream() // stream groups of data by vlf class
        	        .sorted((a, b) -> a.getKey().compareTo(b.getKey())) // this key is vlf class name		
        	        .forEachOrdered(vlfclass ->
        	        {
                	    System.out.format("%nClass : %s%n", vlfclass.getKey());
              	    
                        List<VlfEntry> x = new ArrayList<VlfEntry>(vlfclass.getValue());
                        x.sort((a, b) -> a.smfDateTime.compareTo(b.smfDateTime));
                        for (int i = 0; i < x.size(); i++)
                        {
                            if (i % 50 == 0) // write column headings every 50 lines
                            {
                                writeColumnHeading();
                            }
                            VlfEntry entry = x.get(i);
                            System.out.format(                      
                                "%tF %<tT %8d %8d %8d %8d %8d %8d %8d %8d %8.2f%% %n",                              
                                entry.smfDateTime,
                                entry.smf41mvt, 
                                entry.smf41usd,
                                entry.smf41src,
                                entry.smf41fnd, 
                                entry.smf41add, 
                                entry.smf41del, 
                                entry.smf41trm, 
                                entry.smf41lrg,
                                entry.smf41src > 0 ? (double)entry.smf41fnd/entry.smf41src*100 : 0);       
                        }                             
        	        });        		
            });
    }

    private static void writeColumnHeading()
    {
        System.out.format(                      
                "%n%-19s %8s %8s %8s %8s %8s %8s %8s %8s %9s %n%n",
                "Date / Time",
                "MAXVIRT",
                "Used",
                "Searches",
                "Found",
                "Added",
                "Deleted",
                "Trimmed",
                "Largest",
                "Hit %"
                );
    }

    private static class VlfEntry
    {
    	VlfEntry(Smf41Record record, VlfStatisticsDataSection section)
    	{ 
    		system = record.system();
    		smf41cls = section.smf41cls();
    		smfDateTime = record.smfDateTime();
    		smf41mvt = section.smf41mvt(); 
    		smf41usd = section.smf41usd();
    		smf41src = section.smf41src();
    		smf41fnd = section.smf41fnd();
    		smf41add = section.smf41add();
    		smf41del = section.smf41del();
    		smf41trm = section.smf41trm();
    		smf41lrg = section.smf41lrg();
    	}
    	
    	String system;
    	String smf41cls;
    	LocalDateTime smfDateTime;
		long smf41mvt; 
		long smf41usd;
		long smf41src;
		long smf41fnd;
		long smf41add;
		long smf41del;
		long smf41trm;
		long smf41lrg;
    }
}
