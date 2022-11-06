package com.smfreports;

import java.io.IOException;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;
import com.google.gson.Gson;

public class CicsStatistics 
{
    public static void main(String[] args) throws IOException                                   
    {
        Gson gson = new EasySmfGsonBuilder()
                
                // combine fields into a complete LocalDateTime and exclude individual fields 
                .calculateEntry(StProductSection.class, "time", x -> x.smfstdat().atTime(x.smfstclt()))
                .exclude(StProductSection.class, "smfstdat")
                .exclude(StProductSection.class, "smfstclt")
                
                .createGson();
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0])
                .include(110)) 
        {
            for (SmfRecord record : reader)
            {
                Smf110Record r110 = Smf110Record.from(record);
                {
                    for (StatisticsDataSection stats : r110.statisticsDataSections())
                    {
                        System.out.println(
                                gson.toJson(
                                        new StatsEntry(r110.stProductSection(), stats)));
                    }
                }    
            }                                               
        }
        System.err.println("Done");
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
