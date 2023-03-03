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

/**
 * Write transaction information to JSON for CICS transactions with 
 * an elapsed time greater than a specified threshold (zero for all transactions).
 * 
 * <p>
 * This class uses the Smf2JsonCLI class to provide a command line 
 * interface to handle input and output specified by command line 
 * options and generate the JSON. 
 * 
 */
public class CicsSlowTransactions 
{          
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("Convert transaction information for slow CICS transactions to JSON")
            .includeRecords(110, 1);
        
        // Add a command line option to specify the slow transaction threshold in milliseconds
        smf2JsonCli.options().addOption(
                Option.builder("ms")
                    .longOpt("milliseconds")
                    .hasArg(true)
                    .desc("report transactions longer than this duration (0 for all)")
                    .required()
                    .build());
        
        // Then check the value that the user specified
        // (smf2JsonCli.commandLine will parse the provided args and
        // issue it's own error if e.g. the required argument is missing) 
        double thresholdSeconds = 0;
        try
        {
            thresholdSeconds = Double.parseDouble(
                    smf2JsonCli.commandLine(args).getOptionValue("ms")) 
                        / 1000;
        }
        catch (NumberFormatException ex)
        {
            System.err.println("Failed to parse ms option: " + ex.toString());
            System.exit(0);
        }
        
        // Specify options for creating JSON
        smf2JsonCli.easySmfGsonBuilder()
            .cicsClockDetail(true)
            .includeZeroValues(false)
            .includeEmptyStrings(false)
            .includeUnsetFlags(false)
            ;
        
        smf2JsonCli.start(new CliClient(thresholdSeconds), args);    
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {
        double thresholdSeconds;
        
        CliClient(double thresholdSeconds)
        {
            this.thresholdSeconds = thresholdSeconds;
        }
        
        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
            
            for (PerformanceRecord section : r110.performanceRecords())
            {
                if (thresholdSeconds == 0 ||
                        section.elapsedSeconds() > thresholdSeconds)
                {
                    CompositeEntry entry = new CompositeEntry()
                            .add("time", section.getField(Field.STOP))
                            .add("system", r110.smfsid())
                            .add("smfmnjbn", r110.mnProductSection().smfmnjbn())
                            .add("smfmnprn", r110.mnProductSection().smfmnprn())
                            .add("smfmnspn", r110.mnProductSection().smfmnspn())
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
