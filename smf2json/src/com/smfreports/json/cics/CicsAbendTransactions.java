package com.smfreports.json.cics;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import com.blackhillsoftware.json.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsAbendTransactions 
{      
    double slowSeconds;
    
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("List CICS transactions with ABCODEC or ABCODEO as JSON")
            .includeRecords(110, 1);
        
        smf2JsonCli.easySmfGsonBuilder().cicsClockDetail(false);

        smf2JsonCli.start(new CliClient(), args);    
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {        
        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
    
            String system = r110.smfsid();
            String smfmnjbn = r110.mnProductSection().smfmnjbn();
            String smfmnprn = r110.mnProductSection().smfmnprn();
            String smfmnspn = r110.mnProductSection().smfmnspn();
            
            for (PerformanceRecord section : r110.performanceRecords())
            {
                if (section.getField(Field.ABCODEC).length() > 0 || 
                    section.getField(Field.ABCODEO).length() > 0)
                {
                    CompositeEntry entry = new CompositeEntry()
                            .add("time", section.getField(Field.STOP))
                            .add("system", system)
                            .add("smfmnjbn", smfmnjbn)
                            .add("smfmnprn", smfmnprn)
                            .add("smfmnspn", smfmnspn)
                            .add("event", "abend")
                            .add(section);
                    result.add(entry);
                }
            }
            return result;
        }
        
        @Override
        public List<Object> onEndOfData() {
            System.err.println("Finished");
            return null;
        }
    }
}
