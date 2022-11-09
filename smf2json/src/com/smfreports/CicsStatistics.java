package com.smfreports;

import java.io.*;
import java.lang.invoke.MethodHandles;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;
import com.blackhillsoftware.zutil.io.*;
import com.google.gson.Gson;

import org.apache.commons.cli.*;

public class CicsStatistics 
{
    private static void printUsage(Options options) 
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( MethodHandles.lookup().lookupClass().getName() + 
                " [options] <input-name> <input-name2> ...", 
                System.lineSeparator() +
                "Generate JSON records from CICS statistics SMF data", 
                options, 
                System.lineSeparator() + 
                "<input-name> : File(s) containing SMF records. Binary data, RECFM=U or V[B] including RDW." +
                System.lineSeparator() + 
                "Specify <input-name> or --inDD");
    }
    
    private static Gson gson;
    
    public static void main(String[] args) throws IOException, ParseException                                   
    {
        CommandLine cmd = initOptions(args);    
        
        gson = new EasySmfGsonBuilder()
                
                // combine fields into a complete LocalDateTime and exclude individual fields 
                .calculateEntry(StProductSection.class, "time", x -> x.smfstdat().atTime(x.smfstclt()))
                .exclude(StProductSection.class, "smfstdat")
                .exclude(StProductSection.class, "smfstclt")
                
                .setPrettyPrinting()
                .createGson();
        
        try (SmfRecordReader ddReader = cmd.hasOption("inDD") ?
                SmfRecordReader.fromDD(cmd.getOptionValue("inDD")).include(110) : null;           
             Writer fileWriter = cmd.hasOption("out") ? 
                new BufferedWriter(new FileWriter(cmd.getOptionValue("out"))) : null;
             TextRecordWriter datasetWriter = cmd.hasOption("outDD") ? 
                     TextRecordWriter.newWriterForDD(cmd.getOptionValue("outDD")) : null;
             )
        {
            if (ddReader != null)
            {
                processRecords(ddReader, fileWriter, datasetWriter);  
            }
            for (String name : cmd.getArgList()) // remaining arguments should be input files
            {
                try (SmfRecordReader reader = 
                        SmfRecordReader.fromName(name)
                            .include(110)) 
                {
                    processRecords(reader, fileWriter, datasetWriter);  
                }
            }
        }
        System.err.println("Done");
    }

    private static void processRecords(
            SmfRecordReader reader, 
            Writer fileWriter, 
            TextRecordWriter datasetWriter)
            throws IOException 
    {
        for (SmfRecord record : reader)
        {
            Smf110Record r110 = Smf110Record.from(record);
            {
                for (StatisticsDataSection stats : r110.statisticsDataSections())
                {
                    String json = gson.toJson(
                            new StatsEntry(r110.stProductSection(), stats));
                    if (datasetWriter != null)
                    {
                        datasetWriter.writeLine(json);
                    }
                    else if (fileWriter != null)
                    {
                        fileWriter.write(json);
                        fileWriter.write(System.lineSeparator());
                    }
                    else
                    {
                        System.out.println(json);
                    }
                }
            }    
        }
    }
    
    private static CommandLine initOptions(String[] args) throws ParseException 
    {
        Options options = new Options();
        options.addOption(
                Option.builder("h")
                .longOpt("help")
                .desc("print this message and exit")
                .build());
        options.addOption(
                Option.builder()
                .longOpt("inDD")
                .hasArg(true)
                .desc("input DD name")
                .build());
        options.addOption(
                Option.builder()
                .longOpt("outDD")
                .hasArg(true)
                .desc("output DD name")
                .build());
        options.addOption(
                Option.builder()
                .longOpt("out")
                .hasArg(true)
                .desc("output file name")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (args.length == 0 || cmd.hasOption("help"))
        {
            printUsage(options);
            System.exit(0);
        }
        
        return cmd;
    }
    
    static class StatsEntry
    {
        StatsEntry(StProductSection productSection, StatisticsDataSection statisticsSection)
        {
            this.productSection = productSection;
            this.statisticsSection = statisticsSection;
        }
                
        StProductSection productSection;
        StatisticsDataSection statisticsSection;
    }
}
