package com.smfreports.dataset;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf14.Smf14Record;

public class DatasetActivity 
{
	private static void printUsage() 
	{
		System.out.println("Usage: DatasetActivity [-r] <dataset-pattern> <input-name>");
		System.out.println("-r : Include read activity, otherwise only update activity is reported");                      
		System.out.println("<dataset-pattern> : The dataset pattern to match. Wildcards:");          
		System.out.println("    %  - A single character");          
		System.out.println("    *  - Zero or more characters excluding period i.e. in a single qualifier");          
		System.out.println("    ** - Zero or more characters, can be in multiple qualifiers");          
		System.out.println("<input-name> : filename, //DD:DDNAME or //'DATASET.NAME'");
	}
	
    public static void main(String[] args) throws IOException
    {
    	try 
    	{
	        if (args.length < 2 || args[0].equals("--help") || args[0].equals("-h"))
	        {
	            printUsage();          
	            return;
	        }
	        		
	        int nextArg = 0;
	        
	        boolean includeReadActivity = false;
	        if (args[nextArg].equals("-r"))
	        {
	        	includeReadActivity = true;
	        	nextArg++;
	        }    
	
	        String datasetFilter = args[nextArg++];
	        // create the regular expression from the simplified pattern
	        Pattern pattern = buildPattern(datasetFilter);
	        
	        // Print the input argument and resulting regex, because asterisks in the command 
	        // line can give unexpected and hard to debug results if not quoted correctly 
	        System.out.format("Dataset pattern is: %s%n", datasetFilter);
	        System.out.format("Regex is: %s%n", pattern.pattern());        
	        
	        String inputName = args[nextArg++];
	
	        List<DatasetActivityEvent> events = processData(inputName, pattern, includeReadActivity);
	        
	        writeReport(events);
    	}
        catch (Exception e)
        {
        	printUsage();
        	throw e;
        }
    }

	private static List<DatasetActivityEvent> processData(
			String inputName, 
			Pattern pattern, 
			boolean includeReadActivity) 
					throws IOException 
	{
		List<DatasetActivityEvent> events = new ArrayList<>();
        
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
            	if ((record.recordType() == 14 || record.recordType() == 15)
            		&& Smf14Record.from(record).smf14tds()) // skip type 14/15 for temporary datasets
        		{
        			continue;
        		}

        		DatasetActivityEvent event = DatasetActivityEvent.from(record);
        		
        		if (includeReadActivity || !event.isRead())
        		{
        			if (pattern.matcher(event.getDatasetname()).matches())
        			{
        				events.add(event);
        			}
        			else if (event.getNewname() != null 
        					&& event.getNewname().length() > 0 
        					&& pattern.matcher(event.getNewname()).matches())
        			{
        				events.add(event);        				
        			}
        		}
            }
        }
		return events;
	}
	
	private static void writeReport(List<DatasetActivityEvent> events) {
		Map<String, List<DatasetActivityEvent>> eventsByDataset = events.stream()
        	.collect(Collectors.groupingBy(DatasetActivityEvent::getDatasetname));
        
        eventsByDataset.keySet().stream()
        	.sorted()
        	.forEachOrdered( datasetName ->
        			{
        				eventsByDataset.get(datasetName)
        					.stream()
           					.sorted(Comparator.comparing(DatasetActivityEvent::getTime))
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
     * Create a Regex pattern from a simplified string.
     * To avoid the complexity of regular expressions a
     * simplified syntax is used and converted to a regular expression.
     * An asterisk "*" matches 0 or more of any characters in a single qualifier.
     * Double asterisk "**" matches 0 or more of any characters across qualifiers.
     * A percent sign "%" matches a single character.
     * Regular expression elements that don't use ".", "*", "%" or "$" can
     * also be used.
     */

	private static Pattern buildPattern(String patternString) 
	{
        // strip quotes which might be necessary to avoid the shell or JVM from using the 
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

		// Problem - we want to match zero or more characters with a regex * but we 
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
}
