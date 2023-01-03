package com.smfreports.json.cics;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import com.blackhillsoftware.json.CombinedEntry;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.Field;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsTransactions implements Smf2JsonCLI.Client
{      
    int slowMS;
    
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI.create("Convert CICS transaction information to JSON")
            .includeRecords(110, 1)
            .start(new CicsTransactions(), args);
    }
    
    @Override
    public List<Object> processRecord(SmfRecord record) 
    {
        List<Object> result = new ArrayList<>();
        Smf110Record r110 = Smf110Record.from(record);
        
        IdentificationInfo id = new IdentificationInfo(r110);
        
        for (PerformanceRecord section : r110.performanceRecords())
        {
            if (section.elapsedSeconds() * 1000 > slowMS)
            {
                CombinedEntry entry = new CombinedEntry()
                        .add("time", section.getField(Field.STOP))
                        .add("identificationInfo", id)
                        .add("txInfo", section);
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
                    .desc("report transactions longer than this duration (0 for all)")
                    .required()
                    .build());        
    }
    
    @Override
    public boolean checkCommandLine(CommandLine cmd) 
    {
        try
        {
            slowMS = Integer.parseInt(cmd.getOptionValue("ms"));
        }
        catch (NumberFormatException ex)
        {
            System.err.println("Failed to parse ms option: " + ex.getMessage());
            return false;
        }   
        return true;
    }
    
    private static class IdentificationInfo
    {
        public IdentificationInfo(Smf110Record r110)
        {
            system = r110.smfsid();
            smfmnjbn = r110.mnProductSection().smfmnjbn();
            smfmnprn = r110.mnProductSection().smfmnprn();
            smfmnspn = r110.mnProductSection().smfmnspn();
        }
        
        String system;
        String smfmnjbn;
        String smfmnprn;
        String smfmnspn;
    }
}
