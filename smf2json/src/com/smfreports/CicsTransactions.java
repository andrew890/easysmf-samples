package com.smfreports;

import java.io.IOException;
import java.util.*;

import org.apache.commons.cli.ParseException;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.Smf110Record;
import com.blackhillsoftware.smf.cics.monitoring.PerformanceRecord;
import com.blackhillsoftware.smf2json.Smf2JsonCLI;

public class CicsTransactions implements Smf2JsonCLI.Processor
{      
    public static void main(String[] args) throws IOException, ParseException                                   
    {
        Smf2JsonCLI.create("Convert CICS transaction information to JSON")
            .includeRecords(110, 1)
            .start(new CicsTransactions(), args);
    }
    
    @Override
    public EasySmfGsonBuilder customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder) 
    {
        easySmfGsonBuilder.cicsTxExcludeGroup("DFHRMI");
        easySmfGsonBuilder.exclude("BMCMVCIC");
        return easySmfGsonBuilder;
    }

    @Override
    public List<Object> processRecord(SmfRecord record) 
    {
        List<Object> result = new ArrayList<>();
        Smf110Record r110 = Smf110Record.from(record);
        for (PerformanceRecord section : r110.performanceRecords())
        {
            result.add(section);
        }
        return result;
    }
}
