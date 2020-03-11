package com.smfreports.dataset;
import java.time.LocalDateTime;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf14.Smf14Record;
import com.blackhillsoftware.smf.smf17.Smf17Record;
import com.blackhillsoftware.smf.smf18.Smf18Record;
import com.blackhillsoftware.smf.smf61.Smf61Record;
import com.blackhillsoftware.smf.smf62.Smf62Record;
import com.blackhillsoftware.smf.smf64.Smf64Record;
import com.blackhillsoftware.smf.smf65.Smf65Record;

/**
 * Class used by DatasetActivity report to collect dataset activity events. 
 *
 */
public class DatasetActivityEvent 
	{
		private LocalDateTime time;
		private String datasetname;
		private String newname;
		private String jobname;
		private String event;
		private boolean readEvent = false;
		
		private DatasetActivityEvent()
		{
		}
				
	    public static DatasetActivityEvent from(SmfRecord record) 
		{	
			switch (record.recordType())
			{
			// 14 and 15 use the same mapping
			case 14: // Read
			case 15: // Update
				return DatasetActivityEvent.from(Smf14Record.from(record));
			case 17: // scratch
				return DatasetActivityEvent.from(Smf17Record.from(record));
			case 18: // rename
				return DatasetActivityEvent.from(Smf18Record.from(record));
			case 61: // ICF define
				return DatasetActivityEvent.from(Smf61Record.from(record));
			case 62: // VSAM open
				return DatasetActivityEvent.from(Smf62Record.from(record));
			case 64: // VSAM Status
				return DatasetActivityEvent.from(Smf64Record.from(record));
			case 65: // ICF delete
				return DatasetActivityEvent.from(Smf65Record.from(record));
			}
			throw new IllegalArgumentException("Unexpected record type: " + record.recordType());
		}
		
		private static DatasetActivityEvent from(Smf14Record r14)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
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
		
		private static DatasetActivityEvent from(Smf17Record r17)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
			datasetevent.datasetname = r17.smf17dsn();
			datasetevent.jobname = r17.smf17jbn();
			datasetevent.time = r17.smfDateTime();
			datasetevent.event = "Delete (17)";
			return datasetevent;
		}
		
		private static DatasetActivityEvent from(Smf18Record r18)
		{					
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
			datasetevent.datasetname = r18.smf18ods();
			datasetevent.jobname = r18.smf18jbn();
			datasetevent.time = r18.smfDateTime();
			datasetevent.event =  "Rename (18)";
			datasetevent.newname = r18.smf18nds();
			return datasetevent;

		}
		
		private static DatasetActivityEvent from(Smf61Record r61)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
			datasetevent.datasetname = r61.smf61enm();
			datasetevent.jobname = r61.smf61jnm();
			datasetevent.time = r61.smfDateTime();
			datasetevent.event = "Create (61)";
			return datasetevent;
		}
		
		private static DatasetActivityEvent from(Smf62Record r62)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
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
		
		private static DatasetActivityEvent from(Smf64Record r64)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
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
		
		private static DatasetActivityEvent from(Smf65Record r65)
		{
			DatasetActivityEvent datasetevent = new DatasetActivityEvent();
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