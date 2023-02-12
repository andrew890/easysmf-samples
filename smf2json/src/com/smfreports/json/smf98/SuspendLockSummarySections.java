package com.smfreports.json.smf98;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.smf98.*;
import com.blackhillsoftware.smf.smf98.zos.SuspendLockSummary;
import com.blackhillsoftware.smf2json.cli.Smf2JsonCLI;

public class SuspendLockSummarySections 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI cli = Smf2JsonCLI.create()
                .includeRecords(98,1)
                .description("Format SMF 98 Suspend Lock Summary Sections");
        
        cli.easySmfGsonBuilder()
        
            // we calculate interval start/end values using the Context Summary section
            .exclude(IdentificationSection.class, "smf98intervalEnd")
            .exclude(IdentificationSection.class, "smf98intervalEndEtod")
            .exclude(IdentificationSection.class, "smf98intervalStart")
            .exclude(IdentificationSection.class, "smf98intervalStartEtod")
            .exclude(IdentificationSection.class, "smf98rsd")
            .exclude(IdentificationSection.class, "smf98rst")
            
            ;
                        
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
            Smf98s1Record r98 = Smf98s1Record.from(record);
               
            List<Object> result = new ArrayList<>(); 
            for (SuspendLockSummary suspendLock: r98.suspendLockSummary())
            {     
                result.add(new CompositeEntry()
                        .add("smfid", r98.system())
                        .add("intervalStart", 
                                r98.identificationSection().smf98intervalStart()
                                    .atOffset(r98.contextSummarySection().cvtldto()))
                        .add("intervalEnd", 
                                r98.identificationSection().smf98intervalEnd()
                                    .atOffset(r98.contextSummarySection().cvtldto()))
                        .add(r98.identificationSection())
                        .add(suspendLock))
                        ;
            }
            return result;
        } 
    }
}
