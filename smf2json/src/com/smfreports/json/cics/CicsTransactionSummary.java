package com.smfreports.json.cics;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.blackhillsoftware.json.cics.*;
import com.blackhillsoftware.json.util.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsTransactionSummary 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("Summarize CICS transactions into JSON")
            .processRecordIsThreadsafe(true)
            .includeRecords(110, 1)
            ;
        
        smf2JsonCli.easySmfGsonBuilder()
            .includeZeroValues(false)
            .includeEmptyStrings(false)
            .includeUnsetFlags(false)
            ;
        
        smf2JsonCli.start(new CliClient(), args);
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {
        // keep track of instances seen without a dictionary
        private Set<CicsInstanceId> noDictionary = new HashSet<>();
        
        private Map<HashKey, CicsTransactionGroup> stats = new ConcurrentHashMap<>();
        private CicsTransactionGroupFactory collectorFactory = new CicsTransactionGroupFactory()
                .exclude(Field.START)
                .exclude(Field.STOP)
                .exclude(Field.TRAN)
                .exclude(Field.TTYPE)
                .exclude(Field.RTYPE)
                .exclude(Field.PGMNAME)
                .exclude(Field.SRVCLSNM)
                .exclude(Field.RPTCLSNM)
                .exclude(Field.TCLSNAME)                
                .clockDetail(false)
                ;
        
        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            Smf110Record r110 = Smf110Record.from(record);
            String smfmnprn = r110.mnProductSection().smfmnprn();
            String smfmnspn = r110.mnProductSection().smfmnspn();
            if (r110.haveDictionary())
            {
                r110.performanceRecords().stream()
                    .forEach(performanceRecord ->
                        {
                            HashKey key = HashKey
                                    .of("smfmnprn", smfmnprn)
                                    .and("smfmnspn", smfmnspn)
                                    .and("minute", performanceRecord.getField(Field.STOP).truncatedTo(ChronoUnit.MINUTES))
                                    .and("tran", performanceRecord.getField(Field.TRAN))
                                    .and("ttype", performanceRecord.getField(Field.TTYPE))
                                    .and("rtype", performanceRecord.getField(Field.RTYPE).trim())
                                    .and("pgmname", performanceRecord.getField(Field.PGMNAME))
                                    .and("srvclsnm", performanceRecord.getField(Field.SRVCLSNM))
                                    .and("rptclsnm", performanceRecord.getField(Field.RPTCLSNM))
                                    .and("tclsname", performanceRecord.getField(Field.TCLSNAME))
                                    ;
                            
                            stats.computeIfAbsent(
                                    key, 
                                    value -> collectorFactory.createGroup())
                                 .add(performanceRecord);
                        }
                    );
            }
            else
            {
                // only print the error message once per instance
                if (noDictionary.add(r110.cicsInstance()))
                {
                    System.err.println("No dictionary for: " + r110.cicsInstance().toString() + ", skipping record(s)");
                }
            }
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
}
