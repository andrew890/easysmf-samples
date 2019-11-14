package com.blackhillsoftware.samples;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;

public class RecordCount
{
    public static void main(String[] args) throws IOException
    {
        // use a HashMap to keep the statistics
        Map<Integer, RecordStats> statsMap = new HashMap<Integer, RecordStats>();

        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromName(args[0]))
        {      
            reader.stream()
                .forEach(record -> 
                {
                	int type = record.recordType();
                	int subtype = record.hasSubtypes() ? record.subType() : 0;
                	int key = (type << 16) + subtype;
                    statsMap
                    	.computeIfAbsent(key, typeSubtypeInfo -> new RecordStats(type, subtype))
                        .add(record);    
                });
        }

        writeReport(statsMap);
    }

    private static void writeReport(Map<Integer, RecordStats> statsMap)
    {
        // get the total bytes from all record types
        long totalbytes = statsMap.entrySet().stream()
                .mapToLong(x -> x.getValue().getBytes())
                .sum();

        // write heading
        System.out.format("%5s %8s %11s %11s %7s %9s %9s %9s %n", 
                "Type", "Subtype", "Records", "MB", "Pct", "Min", "Max", "Avg");

        // write data        
        statsMap.entrySet().stream()
        	.map(x -> x.getValue())
            
			 //sort by total bytes descending
			 .sorted(Comparator
					 .comparingLong(RecordStats::getBytes).reversed())
            
        	// alternate sort, by type and subtype
//            .sorted(Comparator
//            		.comparingInt(RecordStats::getRecordtype)
//            		.thenComparingInt(RecordStats::getSubtype))
            
            .forEachOrdered(entry ->
                System.out.format("%5d %8d %11d %11d %7.1f %9d %9d %9d %n", 
                		entry.getRecordtype(),
                        entry.getSubtype(), 
                        entry.getCount(),
                        entry.getBytes() / (1024 * 1024),
                        (float) (entry.getBytes()) / totalbytes * 100, 
                        entry.getMin(), 
                        entry.getMax(),
                        entry.getBytes() / entry.getCount()));

    }

    /**
     * Statistics for a type/subtype combination
     */
    private static class RecordStats
    {
    	RecordStats(int recordtype, int subtype)
    	{
	    	this.recordtype = recordtype;
	    	this.subtype = subtype;
    	}
    	
    	private int recordtype;
    	private int subtype; 	
    	private int count = 0;
    	private long bytes = 0;
    	private int max = 0;
    	private int min = 0;

		int getRecordtype() { return recordtype;	}
		int getSubtype() { return subtype;}
		int getCount() { return count;	}
		long getBytes() { return bytes;}
		int getMax() { return max;	}
		int getMin() { return min;}
    	
        public void add(SmfRecord record)
        {
            count++;
            int length = record.recordLength();
            bytes += length;
            if (min == 0 || length < min)
                min = length;
            if (length > max)
                max = length;
        }
    }
}
