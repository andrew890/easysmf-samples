package com.smfreports.json.cics;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.blackhillsoftware.json.*;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.Smf110Record;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsTransactionSummary2 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("Summarize CICS transactions into JSON")
            .processRecordIsThreadsafe(true)
            .includeRecords(110, 1);  
        
        smf2JsonCli.start(new CliClient(), args);    
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {        
        private Map<Key, CicsTransactionCollector> stats = new ConcurrentHashMap<>();
        private CicsTransactionCollectorFactory collectorFactory = new CicsTransactionCollectorFactory()
                .exclude(Field.START)
                .exclude(Field.STOP)
                .exclude(Field.TRAN)
                .exclude(Field.TTYPE)
                .exclude(Field.RTYPE)
                .exclude(Field.PGMNAME)
                .exclude(Field.SRVCLSNM)
                .exclude(Field.RPTCLSNM)
                .exclude(Field.TCLSNAME);
        
        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            Smf110Record r110 = Smf110Record.from(record);
            r110.performanceRecords().stream()
            	.forEach(performanceRecord ->
	            	{
	            		stats.computeIfAbsent(
	            				new Key(r110, performanceRecord), 
	            				value -> collectorFactory.createCollector(performanceRecord.getDictionary()))
	            		.add(performanceRecord);
	            	}
            	);
            return null;
        }
        
        @Override
        public List<Object> onEndOfData() 
        {
            System.err.println("End of Data");
            
            return stats.entrySet().stream()
            		.map(entry -> 
            			new CompositeEntry()
            				.add(entry.getKey())
            				.add(entry.getValue()))
            		.collect(Collectors.toList());
        }
    }
    
    public static class Key
    {
    	public Key(Smf110Record record, PerformanceRecord section)
    	{
    		smfmnprn = record.mnProductSection().smfmnprn();
    		smfmnspn = record.mnProductSection().smfmnspn();
    		minute = section.getField(Field.STOP).truncatedTo(ChronoUnit.MINUTES);
    		tran = section.getField(Field.TRAN);
    		ttype = section.getField(Field.TTYPE);
    		rtype = section.getField(Field.RTYPE).trim();
    		pgmname = section.getField(Field.PGMNAME);
    		srvclsnm = section.getField(Field.SRVCLSNM);
    		rptclsnm = section.getField(Field.RPTCLSNM);
    		tclsname = section.getField(Field.TCLSNAME);
    	}
    	
		public ZonedDateTime minute;
    	public String tran;
    	public String smfmnprn;
    	public String smfmnspn;
    	public String pgmname;
    	public String ttype;
    	public String rtype;
    	public String srvclsnm;
    	public String rptclsnm;
    	public String tclsname;
    	
    	// hashCode and equals generated using Eclipse.
    	// If the contents of the key change, it is easiest 
    	// to just delete and regenerate these methods.
    	
		@Override
		public int hashCode() {
			return Objects.hash(minute, pgmname, rptclsnm, rtype, smfmnprn, smfmnspn, srvclsnm, tclsname, tran, ttype);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			return Objects.equals(minute, other.minute) && Objects.equals(pgmname, other.pgmname)
					&& Objects.equals(rptclsnm, other.rptclsnm) && Objects.equals(rtype, other.rtype)
					&& Objects.equals(smfmnprn, other.smfmnprn) && Objects.equals(smfmnspn, other.smfmnspn)
					&& Objects.equals(srvclsnm, other.srvclsnm) && Objects.equals(tclsname, other.tclsname)
					&& Objects.equals(tran, other.tran) && Objects.equals(ttype, other.ttype);
		}
    }
    
}
