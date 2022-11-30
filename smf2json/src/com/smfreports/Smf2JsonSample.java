package com.smfreports;

import java.io.IOException;
import java.util.*;
import org.apache.commons.cli.*;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;

public class Smf2JsonSample implements Smf2Json.Processor
{
    private static int outputCount = 0;
    private static int outputMax = 0;
    
    public static void main(String[] args) throws ParseException, IOException                         
    {
        Smf2Json.create("Test SMF2JSON")
            .includeRecords(110)
            .start(new Smf2JsonSample(), args);
    }
    
    @Override
    public void customizeOptions(Options options)
    {
        options.addOption(
                Option.builder()
                .longOpt("max")
                .hasArg(true)
                .desc("maximum output records (approximate)")
                .build());
    }
    
    @Override
    public boolean receiveCommandLine(CommandLine cmd)
    {
        if (cmd.hasOption("max"))
        {
            outputMax = Integer.parseInt(cmd.getOptionValue("max"));
        }
        return true;
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
        if (outputMax == 0 || outputCount < outputMax)
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
            {
                for (StatisticsDataSection stats : r110.statisticsDataSections())
                {
                    result.add(new StatsEntry(r110.stProductSection(), stats));       
                    outputCount++;
                }
            }
            return result;
        }
        else
        {
            return Smf2Json.FINISHED;
        }
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
