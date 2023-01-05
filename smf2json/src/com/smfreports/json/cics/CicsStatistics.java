package com.smfreports.json.cics;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.json.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;
import com.blackhillsoftware.smf2json.cli.*;

public class CicsStatistics implements Smf2JsonCLI.Client
{
    public static void main(String[] args) throws IOException 
    {
        Smf2JsonCLI.create("Convert CICS Statistics Sections to JSON")
            .includeRecords(110)
            .start(new CicsStatistics(), args);
    }

    @Override
    public void customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder)
    {
        // combine fields into a complete LocalDateTime and exclude individual fields 
        easySmfGsonBuilder                
            .calculateEntry(StProductSection.class, "time", 
                    x -> x.smfstdat().atTime(x.smfstclt()))
            .exclude(StProductSection.class, "smfstdat")
            .exclude(StProductSection.class, "smfstclt");
        
        // exclude some other fields
        easySmfGsonBuilder
            .exclude(StProductSection.class, "smfstrsd")
            .exclude(StProductSection.class, "smfstrst")
            .exclude(StProductSection.class, "smfstcst")
            .exclude(StProductSection.class, "smfstrvn")
            .exclude(StProductSection.class, "smfstmfl")
            .exclude(StProductSection.class, "smfstpdn");
    }
    
    @Override
    public List<Object> processRecord(SmfRecord record) 
    {
        Smf110Record r110 = Smf110Record.from(record);
        List<Object> result = new ArrayList<>();
        String system = r110.smfsid();
        for (StatisticsDataSection stats : r110.statisticsDataSections())
        {
            result.add(new CompositeEntry()
                    .add("system", system)
                    .add(r110.stProductSection())
                    .add(stats));
        }
        return result;
    }   
}
