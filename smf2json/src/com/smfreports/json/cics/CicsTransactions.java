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
public class CicsTransactions 
{          
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI smf2JsonCli = Smf2JsonCLI.create()
            .description("Convert transaction information for slow CICS transactions to JSON")
            .includeRecords(110, 1);
        
        setupCommandLineArgs(smf2JsonCli); 
        Configuration config = readCommandLineArgs(args, smf2JsonCli);
        
        // Specify options for creating JSON
        smf2JsonCli.easySmfGsonBuilder()
            .cicsClockDetail(true)
            .includeZeroValues(false)
            .includeEmptyStrings(false)
            .includeUnsetFlags(false)
            ;
        
        smf2JsonCli.start(new CliClient(config), args);    
    }

    private static void setupCommandLineArgs(Smf2JsonCLI smf2JsonCli) 
    {
        smf2JsonCli.options().addOption(
                Option.builder("applid")
                    .longOpt("applid")
                    .hasArgs()
                    .valueSeparator(',')
                    .desc("select specific generic applid")
                    .build());
        
        smf2JsonCli.options().addOption(
                Option.builder("ms")
                    .longOpt("milliseconds")
                    .hasArg(true)
                    .desc("report transactions longer than this duration")
                    .build());
        
        smf2JsonCli.options().addOption(
                Option.builder("abend")
                    .longOpt("abend")
                    .hasArg(false)
                    .desc("only report abended transactions (ABCODEC or ABCODEO has a value)")
                    .build());
        
        smf2JsonCli.options().addOption(
                Option.builder("tx")
                    .longOpt("tran")
                    .hasArgs()
                    .valueSeparator(',')
                    .desc("select specific transactions")
                    .build());
    }
    
    private static Configuration readCommandLineArgs(String[] args, Smf2JsonCLI smf2JsonCli) 
    {
        CommandLine commandLine = smf2JsonCli.commandLine(args);
        Configuration config = new Configuration();

        if (commandLine.hasOption("applid"))
        {
            config.includeApplids = new HashSet<>();
            for (String value : commandLine.getOptionValues("applid"))
            {
                config.includeApplids.add(value);
            }
        }
        
        if (commandLine.hasOption("tx"))
        {
            config.includeTransactions = new HashSet<>();
            for (String value : commandLine.getOptionValues("tx"))
            {
                config.includeTransactions.add(value);
            }
        }
        
        if (commandLine.hasOption("ms"))
        {
            try
            {
                
                config.thresholdSeconds = Double.parseDouble(
                        smf2JsonCli.commandLine(args).getOptionValue("ms")) 
                            / 1000;
            }
            catch (NumberFormatException ex)
            {
                System.err.println("Failed to parse ms option: " + ex.toString());
                System.exit(0);
            }
        }
        
        if (commandLine.hasOption("abend"))
        {
            config.abendsOnly = true;
        }
        
        return config;
    }
    
    private static class Configuration
    {
        Set<String> includeApplids = null;
        Set<String> includeTransactions = null;
        double thresholdSeconds = 0;
        boolean abendsOnly = false;
    }
    
    private static class CliClient implements Smf2JsonCLI.Client
    {
        private Configuration config;
        
        CliClient(Configuration config)
        {
            this.config = config;
        }
        
        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
            
            if (config.includeApplids == null 
                    || config.includeApplids.contains(r110.mnProductSection().smfmnprn()))
            {
                for (PerformanceRecord transaction : r110.performanceRecords())
                {
                    if (includeTransaction(transaction))
                    {
                        CompositeEntry entry = new CompositeEntry()
                                .add("time", transaction.getField(Field.STOP))
                                .add("system", r110.smfsid())
                                .add("smfmnjbn", r110.mnProductSection().smfmnjbn())
                                .add("smfmnprn", r110.mnProductSection().smfmnprn())
                                .add("smfmnspn", r110.mnProductSection().smfmnspn())
                                .add(transaction);
                        result.add(entry);
                    }
                }
            }
            return result;
        }
        
        private boolean includeTransaction(PerformanceRecord transaction)
        {
            if (config.includeTransactions != null 
                    && !config.includeTransactions
                        .contains(transaction.getField(Field.TRAN)))
            {
                return false;
            }
            if (config.thresholdSeconds > 0 
                    && !(transaction.elapsedSeconds() > config.thresholdSeconds))
            {
                return false;
            }
            if (config.abendsOnly  
                    && transaction.getField(Field.ABCODEC).length() == 0  
                    &&  transaction.getField(Field.ABCODEO).length() == 0)
            {
                return false;
            }
            return true;
        }
        
        @Override
        public List<Object> onEndOfData() {
            return null;
        }
    }
}
