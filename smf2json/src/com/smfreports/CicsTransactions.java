package com.smfreports;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.json.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsTransactions implements Smf2JsonCLI.Client
{      
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI.create("Convert CICS transaction information to JSON")
            .includeRecords(110, 1)
            .start(new CicsTransactions(), args);
    }
    
    @Override
    public void customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder) 
    {
        easySmfGsonBuilder.cicsTxExcludeGroup("DFHRMI");
        easySmfGsonBuilder.exclude("BMCMVCIC");
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
