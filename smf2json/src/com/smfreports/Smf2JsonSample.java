package com.smfreports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.Smf110Record;
import com.blackhillsoftware.smf.cics.StProductSection;
import com.blackhillsoftware.smf.cics.statistics.StatisticsDataSection;

public class Smf2JsonSample 
{
    private static int outputCount = 0;
    private static int outputMax = 0;
    
    public static void main(String[] args) throws ParseException, IOException                         
    {
        Smf2Json.create("Test SMF2JSON")
            .includeRecords(110)
            .customizeOptions(Smf2JsonSample::customizeOptions)
            .customizeEasySmfGson(Smf2JsonSample::customizeEasySmfGson)
            .onEndOfData(Smf2JsonSample::endOfdata)
            .receiveCommandLine(Smf2JsonSample::receiveCommandLine)
            .start(args, Smf2JsonSample::processRecord);
    }
    
    public static void customizeOptions(Options options)
    {
        options.addOption(
                Option.builder()
                .longOpt("max")
                .hasArg(true)
                .desc("maximum output records")
                .build());
    }
    
    public static EasySmfGsonBuilder customizeEasySmfGson(EasySmfGsonBuilder easySmfGsonBuilder)
    {
        return easySmfGsonBuilder                
                // combine fields into a complete LocalDateTime and exclude individual fields 
                .calculateEntry(StProductSection.class, "time", x -> x.smfstdat().atTime(x.smfstclt()))
                .exclude(StProductSection.class, "smfstdat")
                .exclude(StProductSection.class, "smfstclt"); 
    }

    public static boolean receiveCommandLine(CommandLine cmd)
    {
        if (cmd.hasOption("max"))
        {
            outputMax = Integer.parseInt(cmd.getOptionValue("max"));
        }
        return true;
    }
    
    public static List<Object> processRecord(SmfRecord record)
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
        return null;
    }
    
    public static List<Object> endOfdata()
    {
        System.err.format("Wrote %d records%n", outputCount);
        return Collections.emptyList();      
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
