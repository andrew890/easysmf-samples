package com.smfreports.dataset;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf14.Smf14Record;
import com.blackhillsoftware.smf.smf17.Smf17Record;
import com.blackhillsoftware.smf.smf18.Smf18Record;
import com.blackhillsoftware.smf.smf61.Smf61Record;
import com.blackhillsoftware.smf.smf62.Smf62Record;
import com.blackhillsoftware.smf.smf64.Smf64Record;
import com.blackhillsoftware.smf.smf65.Smf65Record;

public class DatasetActivity {
	
	private static void printUsage() {
		System.out.println("Usage: DatasetActivity [-r] <dataset-pattern> <input-name>");
		System.out.println("-r : Include read activity, otherwise only update activity is reported");                      
		System.out.println("<dataset-pattern> : The dataset pattern to match. Use wildcards:");          
		System.out.println("    %  - A single character");          
		System.out.println("    *  - Zero or more characters excluding period i.e. in a single qualifier");          
		System.out.println("    ** - Zero or more characters, can be in multiple qualifiers");          
		System.out.println("<input-name> : filename, //DD:DDNAME or //'DATASET.NAME'");
	}
	
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2 || args[0].equals("--help") || args[0].equals("-h"))
        {
            printUsage();          
            return;
        }
        boolean includeReadActivity = false;
        		
        int nextArg = 0;
        
        if (args[nextArg] == "-r")
        {
        	includeReadActivity = true;
        	nextArg++;
        }    

        String datasetFilter = args[nextArg++];
        pattern = buildPattern(datasetFilter);
        // Print the input argument and resulting regex, because asterisk in the command 
        // line can give unexpected and hard to debug results if not quoted correctly 
        
        System.out.format("Dataset pattern is: %s%n", datasetFilter);
        System.out.format("Resulting regex is: %s%n", pattern.pattern());        
        
        String inputName = args[nextArg++];

        List<DatasetEvent> events = processData(inputName, includeReadActivity);
        
        writeReport(events);
    }

	private static List<DatasetEvent> processData(String inputName, boolean includeReadActivity) throws IOException 
	{
		List<DatasetEvent> events = new ArrayList<>();
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(inputName))                
        { 
            reader
	            .include(15)
	            .include(17)
	            .include(18)
	            .include(61)
	            .include(62)
	            .include(64)
	            .include(65);
            if (includeReadActivity)
            {
            	reader.include(14);
            }
            
            for (SmfRecord record : reader) 
            {
            	events.add(eventsFrom(record));

            }
            // If we don't want read events remove them (from non type 14) from the list
        	if (!includeReadActivity)
        	{
        		events = events.stream()
    				.filter(event -> event.isRead() == false)
    				.collect(Collectors.toList());
        	}
        }
		return events;
	}
    
    private static DatasetEvent eventsFrom(SmfRecord record) 
	{
    	// Return events as a list, which means that we can return 1,
    	// 0 or multiple events from a record and the caller handles
    	// them all the same way.
    	// Collections.singletonList is a high performance list 
    	// implementation to contain a single item.
    	
		switch (record.recordType())
		{
		// 14 and 15 use the same mapping
		case 14: // Read
		case 15: // Update
			{
				Smf14Record r14 = Smf14Record.from(record);
				if (pattern.matcher(r14.smfjfcb1().jfcbdsnm()).matches()
						&& !r14.smf14tds()) // ignore temporary datasets 
				{
					return DatasetEvent.from(r14);
				}
				break;
			}
		case 17: // scratch
			{
				Smf17Record r17 = Smf17Record.from(record);
				if (pattern.matcher(r17.smf17dsn()).matches())
				{
					return DatasetEvent.from(r17);
				}				
				break;
			}
		case 18: // rename
			{
				Smf18Record r18 = Smf18Record.from(record);
				if (pattern.matcher(r18.smf18ods()).matches() || pattern.matcher(r18.smf18nds()).matches())
				{
					return DatasetEvent.from(r18);
				}
				break;
			}
		case 61: // ICF define
			{
				Smf61Record r61 = Smf61Record.from(record);
				if (pattern.matcher(r61.smf61enm()).matches())
				{
					return DatasetEvent.from(r61);
				}					
				break;
			}
		case 62: // VSAM open
			{
				Smf62Record r62 = Smf62Record.from(record);
				if (pattern.matcher(r62.smf62dnm()).matches())
				{
					return DatasetEvent.from(r62);
				}	
				break;
			}
		case 64: // VSAM Status
			{
				Smf64Record r64 = Smf64Record.from(record);
				if (pattern.matcher(r64.smf64dnm()).matches())
				{
					return DatasetEvent.from(r64);
				}	
				break;
			}
		case 65: // ICF delete
			{
				Smf65Record r65 = Smf65Record.from(record);
				if (pattern.matcher(r65.smf65enm()).matches())
				{
					return DatasetEvent.from(r65);
				}	
				break;
			}
		}
		return null;
	}
	
	private static void writeReport(List<DatasetEvent> events) {
		Map<String, List<DatasetEvent>> eventsByDataset = events.stream()
        	.collect(Collectors.groupingBy(DatasetEvent::getDatasetname));
        
        eventsByDataset.keySet().stream()
        	.sorted()
        	.forEachOrdered( datasetName ->
        			{
        				eventsByDataset.get(datasetName)
        					.stream()
           					.sorted(Comparator.comparing(DatasetEvent::getTime))
        		        	.forEachOrdered(event ->
                			{
                				System.out.format(
                						"%-44s %-25s %-8s %-15s %-44s%n", 
                						event.getDatasetname(),
                						event.getTime(),
                						event.getJobname(),
                						event.getEvent(),
                						event.getNewname()
                						);
                			});
        				System.out.println();
        			});
	}

    /**
     * Test strings against a pattern.
     * To avoid the complexity of regular expressions a
     * simplified syntax is used and converted to a regular expression.
     * An asterisk "*" matches 0 or more of any characters in a single qualifier.
     * Double asterisk "**" matches 0 or more of any characters across qualifiers.
     * A percent sign "%" matches a single character.
     * Regular expression elements that don't use ".", "*", "%" or "$" can
     * also be used.
     */

    private static Pattern pattern;
    
	private static Pattern buildPattern(String patternString) 
	{
        // strip quotes which might be necessary to avoid shell or JVM from using the 
        // asterisk as a wildcard
        if ((patternString.startsWith("\"") && patternString.endsWith("\""))
        	|| (patternString.startsWith("'") && patternString.endsWith("'")))
        	
        {
        	patternString = patternString.substring(1, patternString.length() -1);
        } 
		
		// escape characters valid in dataset names that have special meaning in regex
		
		// period
		patternString = patternString.replace(".", "\\.");
		
		// dollar sign
		patternString = patternString.replace("$", "\\$");
		
		// translate simplified pattern string to a regex
		
		// % - match a single character excluding period
		patternString = patternString.replace("%", "[^.]");

		// Problem - we want to match zero or more characters with an * but we 
		//           still need to replace * in the original string
		// Solution - we already replaced %, so we know there are no % in the 
		//            pattern string. Use % instead of * temporarily, and
		//            replace it with * later.
				
		// **  - match characters zero or more times
		patternString = patternString.replace("**", ".%"); // % will become *
		
		// * - match characters zero or more times excluding period i.e. single qualifier
		patternString = patternString.replace("*", "[^.]%"); // % will become *
		
		// replace the temporary % with *
		patternString = patternString.replace("%", "*");

		return Pattern.compile("^" + patternString + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}
	
	private static class DatasetEvent 
	{
		private LocalDateTime time;
		private String datasetname;
		private String newname;
		private String jobname;
		private String event;
		private boolean readEvent = false;
		
		private DatasetEvent()
		{
		}
		
		public static DatasetEvent from(Smf14Record r14)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r14.smfjfcb1().jfcbdsnm();
			datasetevent.jobname = r14.smf14jbn();
			datasetevent.time = r14.smfDateTime();
			if (r14.recordType() == 15)
			{
				datasetevent.event = "Update (15)";
				
			}
			else
			{
				datasetevent.event = "Read (14)";
				datasetevent.readEvent = true;
			}

			return datasetevent;
		}
		public static DatasetEvent from(Smf17Record r17)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r17.smf17dsn();
			datasetevent.jobname = r17.smf17jbn();
			datasetevent.time = r17.smfDateTime();
			datasetevent.event = "Delete (17)";
			return datasetevent;
		}
		public static DatasetEvent from(Smf18Record r18)
		{					
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r18.smf18ods();
			datasetevent.jobname = r18.smf18jbn();
			datasetevent.time = r18.smfDateTime();
			datasetevent.event =  "Rename (18)";
			datasetevent.newname = r18.smf18nds();
			return datasetevent;

		}
		public static DatasetEvent from(Smf61Record r61)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r61.smf61enm();
			datasetevent.jobname = r61.smf61jnm();
			datasetevent.time = r61.smfDateTime();
			datasetevent.event = "Create (61)";
			return datasetevent;
		}
		public static DatasetEvent from(Smf62Record r62)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r62.smf62dnm();
			datasetevent.jobname = r62.smf62jbn();
			datasetevent.time = r62.smfDateTime();
			if ((r62.statisticsSection().smf62mc1() & 0x02) != 0)
			{
				datasetevent.event = "Update (62)";
				
			}
			else
			{
				datasetevent.event = "Read (62)";
				datasetevent.readEvent = true;
			}
			return datasetevent;
		}
		public static DatasetEvent from(Smf64Record r64)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r64.smf64dnm();
			datasetevent.jobname = r64.smf64jbn();
			datasetevent.time = r64.smfDateTime();
			if ((r64.statisticsSection().smf64mc1() & 0x02) != 0)
			{
				datasetevent.event = "Update (64)";				
			}
			else
			{
				datasetevent.event = "Read (64)";
				datasetevent.readEvent = true;
			}
			return datasetevent;
		}
		public static DatasetEvent from(Smf65Record r65)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r65.smf65enm();
			datasetevent.jobname = r65.smf65jnm();
			datasetevent.time = r65.smfDateTime();
			datasetevent.event = "Delete (65)";
			return datasetevent;
		}

		public String getDatasetname() {
			return datasetname;
		}
		
		public String getNewname() 
		{
			return newname != null ? newname : "";
		}

		public String getJobname() {
			return jobname;
		}

		public LocalDateTime getTime() {
			return time;
		}

		public String getEvent() {
			return event;
		}
		
		public boolean isRead() {
			return readEvent;
		}
		
	}
	
}
