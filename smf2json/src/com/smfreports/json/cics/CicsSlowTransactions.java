package com.smfreports.json.cics;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsSlowTransactions 
{      
    double slowSeconds;
    
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("Convert transaction information for slow CICS transactions to JSON")
            .includeRecords(110, 1);
        
        smf2JsonCli.options().addOption(
                Option.builder("ms")
                    .longOpt("milliseconds")
                    .hasArg(true)
                    .valueSeparator('=')
                    .desc("report transactions longer than this duration (0 for all)")
                    .required()
                    .build());
        
        smf2JsonCli.easySmfGsonBuilder()
            .cicsClockDetail(true)
            .includeZeroValues(false)
            .includeEmptyStrings(false)
            ;
        
        double slowSeconds = 0;
        try
        {
            slowSeconds = Double.parseDouble(
                    smf2JsonCli.commandLine(args).getOptionValue("ms")) 
                        / 1000;
        }
        catch (NumberFormatException ex)
        {
            System.err.println("Failed to parse ms option: " + ex.toString());
            System.exit(0);
        }
        smf2JsonCli.start(new CliClient(slowSeconds), args);    
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {
        double slowSeconds;
        
        CliClient(double slowSeconds)
        {
            this.slowSeconds = slowSeconds;
        }
        
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
                if (section.elapsedSeconds() > slowSeconds)
                {
                    CompositeEntry entry = new CompositeEntry()
                            .add("time", section.getField(Field.STOP))
                            .add("system", system)
                            .add("smfmnjbn", smfmnjbn)
                            .add("smfmnprn", smfmnprn)
                            .add("smfmnspn", smfmnspn)
                            .add(section);
                    result.add(entry);
                }
            }
            return result;
        }
        
        @Override
        public List<Object> onEndOfData() {
            return null;
        }
    }
}
