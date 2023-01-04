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

public class CicsSlowTransactions implements Smf2JsonCLI.Client
{      
    double slowSeconds;
    
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI.create("Convert transaction information for slow CICS transactions to JSON")
            .includeRecords(110, 1)
            .start(new CicsSlowTransactions(), args);
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
    public void customizeOptions(Options options) 
    {
        options.addOption(
                Option.builder("ms")
                    .longOpt("milliseconds")
                    .hasArg(true)
                    .valueSeparator('=')
                    .desc("report transactions longer than this duration (0 for all)")
                    .required()
                    .build());        
    }
    
    @Override
    public boolean validateCommandLine(CommandLine cmd) 
    {
        try
        {
            slowSeconds = (double)Integer.parseInt(cmd.getOptionValue("ms")) / 1000;
        }
        catch (NumberFormatException ex)
        {
            System.err.println("Failed to parse ms option: " + ex.toString());
            return false;
        }   
        return true;
    }
}
