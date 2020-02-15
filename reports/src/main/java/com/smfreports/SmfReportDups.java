package com.smfreports;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;

public class SmfReportDups
{
	private static void printUsage() {
		System.out.println("Usage: SmfReportDups <input-file> [input-file2 ...]");
		System.out.println("");
		System.out.println("Search for duplicated data in input-file.");
		System.out.println("");
		System.out.println("  input-file        File containing SMF records. Binary data, RECFM=U or");
		System.out.println("                    V[B] records including RDW.");
		System.out.println("  input-file2 ...   Additional input file(s), to search for duplicates");
		System.out.println("                    across multiple files.");
		System.out.println("");
		System.out.println("Records and duplicate records are grouped and counted by system and minute.");
		System.out.println("If the number of duplicate records in a minute is greater than or equal");
		System.out.println("to the number of unique records in that minute, data for that minute is");
		System.out.println("likely to have been duplicated and the counts for that minute are reported.");
		System.out.println("");
		System.out.println("Specific record types might also be included multiple times in the data.");
		System.out.println("Data for each minute is checked by record type. Again, any record type");
		System.out.println("where the number of duplicate records is greater than or equal to the");
		System.out.println("number of unique records might be duplicated data so the counts are");
		System.out.println("reported.");
		System.out.println("Minutes appearing in the first part of the report are excluded to avoid");
		System.out.println("reporting every record type for those minutes.");
	}
	
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException
    {
    	if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h"))
    	{
    		printUsage();
    		System.exit(0);
    	}
    	
    	MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    	Set<BigInteger> recordHashes = new HashSet<BigInteger>();
    	
    	// Map to count by SystemID->Minute
    	Map< String, Map<LocalDateTime, RecordStats>> allByMinute = new HashMap<>();
    	
    	// Map to Count by SystemID->Record Type->Minute 
    	Map< String, Map<LocalDateTime, Map<Integer, RecordStats>>> byMinuteByType = new HashMap<>();
    	   	
    	int in = 0;
    	int dups = 0;
    	try
    	{
	    	for (int i = 0; i < args.length; i++)
	    	{
		        try (SmfRecordReader reader = SmfRecordReader.fromName(args[i]))                
		        {
		            for (SmfRecord record : reader)
		            {
		            	in++;
		            	String system = record.system();
		            	LocalDateTime minute = record.smfDateTime()
		            			.truncatedTo(ChronoUnit.MINUTES);
		            	Integer recordtype = record.recordType();
		            	
		            	RecordStats minuteStats = allByMinute
		            			.computeIfAbsent(system, key -> new HashMap<>())
		            			.computeIfAbsent(minute, key -> new RecordStats(null, minute));
		            	
		            	RecordStats minuteRecordTypeStats = byMinuteByType
		            			.computeIfAbsent(system, key -> new HashMap<>())
		            			.computeIfAbsent(minute, key -> new HashMap<>())
		            			.computeIfAbsent(recordtype, key -> new RecordStats(recordtype, minute));
		            	
		                if (recordHashes.add(new BigInteger(sha256.digest(record.getBytes()))))
		                {
		                	minuteStats.countUnique();
		                	minuteRecordTypeStats.countUnique();
		                }
		                else
		                {
		                	dups++;
		                	minuteStats.countDuplicate();
		                	minuteRecordTypeStats.countDuplicate();
		                }
		            }
		        }
	    	}
    	}
        catch (Exception e)
        {
        	printUsage();
        	throw e;
        }
    	
        System.out.format("Finished, %d records in, %d duplicates.%n", in, dups);
        
        List<String> systems = allByMinute.keySet().stream()
        		.sorted()
        		.collect(Collectors.toList());
        
        for (String system : systems)
        {
	        List<RecordStats> duplicateMinutes = allByMinute.get(system).values().stream()
	        		.filter(entry -> entry.dupPercent() >= 100)
	        		.sorted(Comparator.comparing(RecordStats::getMinute))
	        		.collect(Collectors.toList());
	        
	        if (!duplicateMinutes.isEmpty())
	        {
	            System.out.format("%nSystem : %s%n", system);
	        	
	        	System.out.format("%n%-20s %8s %8s %6s%n%n",
	        			"Minute",
	        			"Records",
	        			"Dup",
	        			"Dup%"
	        			);
	            for (RecordStats minuteEntry : duplicateMinutes)
	            {
	            	System.out.format("%-20s %8d %8d %6.0f%n", 
	            			minuteEntry.getMinute(), 
	            			minuteEntry.getTotal(), 
	            			minuteEntry.getDuplicates(),
	            			minuteEntry.dupPercent());
	            	
	            	// We don't want to report duplicates for every record type in this minute.
	            	// Remove the entry for this minute from the map by type.
	            	byMinuteByType.get(system)
	            		.remove(minuteEntry.getMinute());
	            }
	        }
	        
	        // Flatmap into one list and select entries with duplicates.
	        // Sort by minute and record type
	
	        List<RecordStats> duplicatesByMinuteByType = byMinuteByType.get(system).values().stream()
	        		.flatMap(entry -> entry.values().stream())
	        		.filter(entry -> entry.dupPercent() >= 100)
	        		.sorted(Comparator.comparing(RecordStats::getMinute)
	        				.thenComparing(RecordStats::getRecordtype))
	        		.collect(Collectors.toList());
	        
	        if (!duplicatesByMinuteByType.isEmpty())
	        {
	            System.out.format("%nSystem : %s%n", system);
	        	System.out.format("%n%-20s %4s %8s %8s %6s%n%n",
	        			"Minute",
	        			"Type",
	        			"Records",
	        			"Dup",
	        			"Dup%"
	        			);
	        	
	            for (RecordStats recordTypeEntry : duplicatesByMinuteByType)
	            {
	            	
	            	System.out.format("%-20s %4d %8d %8d %6.0f%n",
	            			recordTypeEntry.getMinute(),
	            			recordTypeEntry.getRecordtype(), 
	            			recordTypeEntry.getTotal(), 
	            			recordTypeEntry.getDuplicates(),
	            			recordTypeEntry.dupPercent());
	            }
	        }
        }

    }
    
    private static class RecordStats
    {
    	RecordStats(Integer recordtype, LocalDateTime minute)
    	{
    		this.recordtype = recordtype;
    		this.minute = minute;
    	}
    	
    	private Integer recordtype;
    	private int unique = 0;
    	private int duplicates = 0;
    	private LocalDateTime minute;

        public void countUnique()
        {
            unique++;
        }
        
        public void countDuplicate()
        {
            duplicates++;
        }

		private Integer getRecordtype() {
			return recordtype;
		}
		
		private LocalDateTime getMinute() {
			return minute;
		}

		private int getTotal() {
			return unique + duplicates;
		}
		
		private int getDuplicates() {
			return duplicates;
		}
		
		private double dupPercent() 
		{
			return (double)duplicates/unique * 100;
		}
    }
}
