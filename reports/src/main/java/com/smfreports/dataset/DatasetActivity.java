package com.smfreports.dataset;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.blackhillsoftware.smf.smf14.Smf14Record;
import com.blackhillsoftware.smf.smf17.Smf17Record;
import com.blackhillsoftware.smf.smf18.Smf18Record;
import com.blackhillsoftware.smf.smf61.Smf61Record;
import com.blackhillsoftware.smf.smf62.Smf62Record;
import com.blackhillsoftware.smf.smf64.Smf64Record;
import com.blackhillsoftware.smf.smf65.Smf65Record;

public class DatasetActivity {
	
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("Usage: DatasetActivity <input-name> <dataset-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        pattern = buildPattern(args[0]);
    }
	
    /**
     * Test strings against a pattern.
     * To avoid the complexity of regular expressions a
     * simplified syntax is used and converted to a regular expression.
     * Existing dots "." i.e. separators between dataset qualifiers are
     * escaped.
     * An asterisk "*" matches 1 or more of any characters.
     * A question mark "?" matches a single character.
     * Regular expression elements that don't use ".", "*" or "?" can
     * be used.    
     *
     */

    private static Pattern pattern;
    
	private static Pattern buildPattern(String patternString) {
		patternString = patternString.replace(".", "\\.");
		patternString = patternString.replace("?", ".");
		patternString = patternString.replace("*", ".+");
		return Pattern.compile("^" + patternString + "$");
	}
	
	public static boolean matchValue(String value) 
	{
		return pattern.matcher(value).matches();
	}

	static class DatasetEvent 
	{
		private DatasetEvent()
		{
		}
		public static DatasetEvent from(Smf14Record r14)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r14.smfjfcb1().jfcbdsnm();
			datasetevent.jobname = r14.smf14jbn();
			datasetevent.time = r14.smfDateTime();
			datasetevent.event = r14.recordType() == 15 ? "Update" : "Read";
			return datasetevent;
		}
		public static DatasetEvent from(Smf17Record r17)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r17.smf17dsn();
			datasetevent.jobname = r17.smf17jbn();
			datasetevent.time = r17.smfDateTime();
			datasetevent.event = "Delete";
			return datasetevent;
		}
		public static List<DatasetEvent> from(Smf18Record r18)
		{
			List<DatasetEvent> result = new ArrayList<>(2);
			
			// use old dataset name for scratch event and new dataset name for update event
			DatasetEvent oldnamescratch = new DatasetEvent();
			oldnamescratch.datasetname = r18.smf18ods();
			oldnamescratch.jobname = r18.smf18jbn();
			oldnamescratch.time = r18.smfDateTime();
			oldnamescratch.event =  "Delete";
			
			DatasetEvent newnamecreate = new DatasetEvent();
			newnamecreate.datasetname = r18.smf18nds();
			newnamecreate.jobname = r18.smf18jbn();
			newnamecreate.time = r18.smfDateTime();
			newnamecreate.event =  "Update";
			
			result.add(oldnamescratch);
			result.add(newnamecreate);
			
			return result;

		}
		public static DatasetEvent from(Smf61Record r61)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r61.smf61enm();
			datasetevent.jobname = r61.smf61jnm();
			datasetevent.time = r61.smfDateTime();
			datasetevent.event = "Create";
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
				datasetevent.event = "Update";
				
			}
			else
			{
				datasetevent.event = "Read";
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
				datasetevent.event = "Update";				
			}
			else
			{
				datasetevent.event = "Read";
			}
			return datasetevent;
		}
		public static DatasetEvent from(Smf65Record r65)
		{
			DatasetEvent datasetevent = new DatasetEvent();
			datasetevent.datasetname = r65.smf65enm();
			datasetevent.jobname = r65.smf65jnm();
			datasetevent.time = r65.smfDateTime();
			datasetevent.event = "Delete";
			return datasetevent;
		}

		public String getDatasetname() {
			return datasetname;
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

		private LocalDateTime time;
		private String datasetname;
		private String jobname;
		private String event;
	}
	
}
