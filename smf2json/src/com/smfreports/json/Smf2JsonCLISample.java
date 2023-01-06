package com.smfreports.json;

import java.io.*;
import java.util.*;
import org.apache.commons.cli.*;

import com.blackhillsoftware.smf2json.cli.*;
import com.blackhillsoftware.json.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.statistics.*;

public class Smf2JsonCLISample 
{    
    public static void main(String[] args) throws ParseException, IOException                         
    {
        Smf2JsonCLI cli = Smf2JsonCLI.create(args)
            .description("Test SMF2JSON")
            .includeRecords(110);
        
        cli.options()
            .addOption(
                Option.builder()
                .longOpt("max")
                .hasArg(true)
                .desc("maximum output records (approximate)")
                .build());
        
        cli.easySmfGsonBuilder()
             // combine fields into a complete LocalDateTime and exclude individual fields 
            .calculateEntry(StProductSection.class, "time", x -> x.smfstdat().atTime(x.smfstclt()))
            .exclude(StProductSection.class, "smfstdat")
            .exclude(StProductSection.class, "smfstclt");
        
        CliClient client = new CliClient();
        
        if (cli.commandLine().hasOption("max"))
        {
            client.setOutputMax(Integer.parseInt(cli.commandLine().getOptionValue("max")));
        }
        
        cli.start(client);
    }
    
    static class CliClient implements Smf2JsonCLI.Client
    {
        private int outputCount = 0;
        private int outputMax = 0;
        
        public void setOutputMax(int outputMax) 
        {
            this.outputMax = outputMax;
        }
        
        @Override
        public List<Object> processRecord(SmfRecord record)
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
            {
                for (StatisticsDataSection stats : r110.statisticsDataSections())
                {
                    result.add(
                            new CompositeEntry(
                                    r110.stProductSection(),
                                    stats));       
                    outputCount++;
                    if (outputMax != 0 && outputCount >= outputMax)
                    {
                        result.add(Smf2JsonCLI.FINISHED);
                        return result;
                    }
                }
            }
            return result;
        }
        
        @Override
        public List<Object> onEndOfData() {
            return null;
        }
    }
}
