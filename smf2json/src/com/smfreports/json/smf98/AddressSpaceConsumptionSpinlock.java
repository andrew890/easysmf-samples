package com.smfreports.json.smf98;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.smf98.*;
import com.blackhillsoftware.smf.smf98.zos.AddressSpaceConsumption;
import com.blackhillsoftware.smf.smf98.zos.AddressSpaceSpinLock;
import com.blackhillsoftware.smf.smf98.zos.AsidInfo;
import com.blackhillsoftware.smf2json.cli.Smf2JsonCLI;

public class AddressSpaceConsumptionSpinlock 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI cli = Smf2JsonCLI.create()
                .includeRecords(98,1)
                .description("Format SMF 98 Spin Lock Sections from Address Space Consumption Sections");
        
        cli.easySmfGsonBuilder()
            //.setPrettyPrinting()
        
            // we calculate interval start/end values using the Context Summary section
            .exclude(IdentificationSection.class, "smf98intervalEnd")
            .exclude(IdentificationSection.class, "smf98intervalEndEtod")
            .exclude(IdentificationSection.class, "smf98intervalStart")
            .exclude(IdentificationSection.class, "smf98intervalStartEtod")
            .exclude(IdentificationSection.class, "smf98rsd")
            .exclude(IdentificationSection.class, "smf98rst")
            
            // other uninteresting fields
            .exclude(IdentificationSection.class, "smf98jbn")
            .exclude(IdentificationSection.class, "smf98stp")
            
            // substitute flags field with hex formatted string
            .exclude(AsidInfo.class, "flags")
            .calculateEntry(AsidInfo.class, "flags", x -> String.format("0x%02X", x.flags()))
            
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
            for (AddressSpaceConsumption consume: r98.addressSpaceConsumption())
            {     
                for (AddressSpaceSpinLock spinlockSection : consume.addressSpaceSpinLock())
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
                            .add("priorityBucket", consume.priorityBucket())
                            .add("procClass", consume.procClass())
                            .add("subBucket", consume.subBucket())
                            .add(spinlockSection))
                            ;
                }
            }
            return result;
        } 
    }
}
