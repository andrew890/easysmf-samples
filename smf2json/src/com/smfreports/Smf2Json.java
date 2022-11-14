package com.smfreports;

import java.io.*;
import java.util.*;
import java.util.function.*;
import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.zutil.io.*;
import com.google.gson.Gson;

import org.apache.commons.cli.*;

public class Smf2Json 
{
    private void printUsage(Options options) 
    {
        StackTraceElement[] stack = Thread.currentThread ().getStackTrace ();
        StackTraceElement main = stack[stack.length - 1];
        
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( main.getClassName() + 
                " [options] <input-name> <input-name2> ...", 
                System.lineSeparator() +
                description, 
                options, 
                System.lineSeparator() + 
                "<input-name> : File(s) containing SMF records. Binary data, RECFM=U or V[B] including RDW." +
                System.lineSeparator() + 
                "Specify <input-name> or --inDD");
    }
    
    private Smf2Json(String description)
    {
        this.description = description;
    }
    
    public static Smf2Json create(String description)
    {
        return new Smf2Json(description);
    }
    
    private String description;
    private Gson gson;

    private List<Integer> smfTypes = new ArrayList<>();
    private List<SmfTypeSubtype> smfTypeSubtypes = new ArrayList<>();
    
    private Function<EasySmfGsonBuilder, EasySmfGsonBuilder> easysmfGsonCustomizer = null;
    private Consumer<Options> optionsCustomizer = null;
    private Function<CommandLine, Boolean> checkCommand = null;
    private Supplier<List<Object>> onEndOfData = null;
        
    private Options initOptions(String[] args)
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
        options.addOption(
                Option.builder()
                .longOpt("pretty")
                .hasArg(false)
                .desc("pretty print json")
                .build());
           
        if (optionsCustomizer != null)
        {
            optionsCustomizer.accept(options);
        }
        
        return options;
    }    
    
    public Smf2Json includeRecords(int smfType)
    {
        smfTypes.add(smfType);
        return this;
    }
    
    public Smf2Json includeRecords(int smfType, int subType)
    {
        smfTypeSubtypes.add(new SmfTypeSubtype(smfType, subType));
        return this;
    }
    
    public Smf2Json customizeEasySmfGson(Function<EasySmfGsonBuilder, EasySmfGsonBuilder> builder) {
        easysmfGsonCustomizer = builder;
        return this;
    }

    public Smf2Json customizeOptions(Consumer<Options> options) {
        optionsCustomizer = options;
        return this;
    }

    public Smf2Json receiveCommandLine(Function<CommandLine, Boolean> cmd) {
        checkCommand = cmd;
        return this;
    }

    public Smf2Json onEndOfData(Supplier<List<Object>> processor) {
        onEndOfData = processor;
        return this;
    }
        
    public void start(String[] args, Function<SmfRecord, List<Object>> processor) throws ParseException, IOException 
    {    
        Options options = initOptions(args);
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        if (args.length == 0 || cmd.hasOption("help"))
        {
            printUsage(options);
            System.exit(0);
        }
        
        if (checkCommand == null || checkCommand.apply(cmd))
        {
            EasySmfGsonBuilder builder = new EasySmfGsonBuilder();      
            
            if(cmd.hasOption("pretty"))
            {
                builder.setPrettyPrinting();
            }
            
            builder = easysmfGsonCustomizer.apply(builder);
            gson = builder.createGson();
            
            try (SmfRecordReader ddReader = cmd.hasOption("inDD") ?
                    SmfRecordReader.fromDD(cmd.getOptionValue("inDD")) : null;           
                 Writer fileWriter = cmd.hasOption("out") ? 
                    new BufferedWriter(new FileWriter(cmd.getOptionValue("out"))) : null;
                 TextRecordWriter datasetWriter = cmd.hasOption("outDD") ? 
                         TextRecordWriter.newWriterForDD(cmd.getOptionValue("outDD")) : null;
                 )
            {
                if (ddReader != null)
                {
                    setRecordTypes(ddReader);
                    for (SmfRecord record : ddReader)
                    {
                        List<Object> result = processor.apply(record);
                        if (result == null) break;
                        writeJson(fileWriter, datasetWriter, result);
                    }
                }
                for (String name : cmd.getArgList()) // remaining arguments should be input files
                {
                    try (SmfRecordReader reader = 
                            SmfRecordReader.fromName(name)) 
                    {
                        setRecordTypes(reader);
                        for (SmfRecord record : reader)
                        {
                            List<Object> result = processor.apply(record);
                            if (result == null) break;
                            writeJson(fileWriter, datasetWriter, result);    
                        }    
                    }
                }
                if (onEndOfData != null)
                {
                    writeJson(fileWriter, datasetWriter, onEndOfData.get());
                }
            }
        }
    }

    private void setRecordTypes(SmfRecordReader reader) 
    {
        for (int smfType : smfTypes)
        {
            reader.include(smfType);
        }
        for (SmfTypeSubtype smfType : smfTypeSubtypes)
        {
            reader.include(smfType.smfType, smfType.subType);
        }
    }

    private void writeJson(Writer fileWriter, TextRecordWriter datasetWriter, List<Object> objects) throws IOException 
    {
        if (objects != null)
        {
            for (int i=0; i < objects.size(); i++)
            {
                String json = gson.toJson(objects.get(i));
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

    private static class SmfTypeSubtype
    {
        int smfType;
        int subType;
        SmfTypeSubtype(int smfType, int subType)
        {
            this.smfType = smfType;
            this.subType = subType;
        }        
    }
}
