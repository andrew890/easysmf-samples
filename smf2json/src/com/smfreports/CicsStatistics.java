package com.smfreports;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.blackhillsoftware.json.CombinedEntry;
import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;
import com.blackhillsoftware.smf2json.Smf2JsonCLI;

import org.apache.commons.cli.*;

public class CicsStatistics implements Smf2JsonCLI.Processor
{
    public static void main(String[] args) throws IOException, ParseException 
    {
        Smf2JsonCLI.create("Convert CICS Statistics Sections to JSON")
            .includeRecords(110)
            .start(new CicsStatistics(), args);
    }

    @Override
    public EasySmfGsonBuilder customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder)
    {
        return easySmfGsonBuilder                
                // combine fields into a complete LocalDateTime and exclude individual fields 
                .calculateEntry(StProductSection.class, "time", x -> x.smfstdat().atTime(x.smfstclt()))
                .exclude(StProductSection.class, "smfstdat")
                .exclude(StProductSection.class, "smfstclt"); 
    }
    
    @Override
    public List<Object> processRecord(SmfRecord record) 
    {
        Smf110Record r110 = Smf110Record.from(record);
        List<Object> result = new ArrayList<>();
        for (StatisticsDataSection stats : r110.statisticsDataSections())
        {
            
            result.add(new CombinedEntry()
                    .add("productSection", r110.stProductSection())
                    .add("statisticsSection", stats));
        }
        return result;
    }   
}
