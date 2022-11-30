package com.smfreports;

import java.io.*;
import java.util.*;
import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.google.gson.Gson;

import org.apache.commons.cli.*;

public class Smf2Json
{    
    public interface Processor
    {
        public List<Object> processRecord(SmfRecord record);
        public default List<Object> endOfData(){return Collections.emptyList();};
        public default boolean receiveCommandLine(CommandLine cmd) throws ParseException {return true;};
        public default EasySmfGsonBuilder customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder) {return easySmfGsonBuilder;};
        public default void customizeOptions(Options options) {};
    }
    
    private void printUsage(Options options) 
    {
        // find main[] class name
        StackTraceElement[] stack = Thread.currentThread ().getStackTrace ();
        StackTraceElement main = stack[stack.length - 1];
        
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( main.getClassName() + 
                " [options] <input-name> <input-name2> ...", 
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
    
    Smf2Json.Processor processor;
    
    private String description;
    private Gson gson;

    private List<Smf2JsonInput> inputs = new ArrayList<>();
    private List<Integer> smfTypes = new ArrayList<>();
    private List<SmfTypeSubtype> smfTypeSubtypes = new ArrayList<>();    
    
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
        
        processor.customizeOptions(options);
        
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
        
    public void start(Smf2Json.Processor processor, String[] args) throws ParseException, IOException 
    {    
        Objects.requireNonNull(processor);
        Objects.requireNonNull(args);
        
        this.processor = processor;
        Options options = initOptions(args);
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        if (args.length == 0 || cmd.hasOption("help"))
        {
            printUsage(options);
            System.exit(0);
        }
        
        if (processor.receiveCommandLine(cmd))
        {
            EasySmfGsonBuilder builder = new EasySmfGsonBuilder();      
            
            if(cmd.hasOption("pretty"))
            {
                builder.setPrettyPrinting();
            }
            
            builder = processor.customizeEasySmfGson(builder);
            gson = builder.createGson();
            
            if (cmd.hasOption("inDD"))
            {
                inputs.add(Smf2JsonInput.dd(cmd.getOptionValue("inDD")));
            }
            for (String name : cmd.getArgList()) // remaining arguments should be input files
            {
                inputs.add(Smf2JsonInput.file(name));
            }
            
            try (Smf2JsonWriter writer = 
                        cmd.hasOption("out")   ? Smf2JsonWriter.forFile(cmd.getOptionValue("out")) : 
                        cmd.hasOption("outDD") ? Smf2JsonWriter.forDD(cmd.getOptionValue("outDD")) :
                                                 Smf2JsonWriter.forStdout();
                 )
            {
                for (Smf2JsonInput input : inputs) 
                {
                    try (SmfRecordReader reader = input.getReader())
                    {
                        setRecordTypes(reader);
                        for (SmfRecord record : reader)
                        {
                            List<Object> result = processor.processRecord(record);
                            if (result == null) break;
                            writeJson(writer, result);    
                        }    
                    }
                }
                writeJson(writer, processor.endOfData());               
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

    private void writeJson(Smf2JsonWriter writer, List<Object> objects) throws IOException 
    {
        if (objects != null)
        {
            for (int i=0; i < objects.size(); i++)
            {
                String json = gson.toJson(objects.get(i));
                writer.writeLine(json);
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
