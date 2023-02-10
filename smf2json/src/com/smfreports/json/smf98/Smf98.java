package com.smfreports.json.smf98;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.smf98.Smf98Record;
import com.blackhillsoftware.smf2json.cli.Smf2JsonCLI;

public class Smf98 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI cli = Smf2JsonCLI.create()
                .includeRecords(98,1)
                .description("SMF 98 Records");
        
        cli.easySmfGsonBuilder().setPrettyPrinting();
                
        cli.start(new CliClient(), args);
    }

    private static class CliClient implements Smf2JsonCLI.Client
    {
        @Override
        public List<Object> onEndOfData() 
        {
            return null;
        }

        @Override
        public List<Object> processRecord(SmfRecord record)
        {
            List<Object> result = new ArrayList<>();
            result.add(Smf98Record.from(record));
            result.add(Smf2JsonCLI.FINISHED);
            return result;
        }
    
    }
    
}
