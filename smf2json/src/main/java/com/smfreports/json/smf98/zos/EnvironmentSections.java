package com.smfreports.json.smf98.zos;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.smf98.*;
import com.blackhillsoftware.smf.smf98.zos.*;
import com.blackhillsoftware.smf2json.cli.Smf2JsonCLI;

/**
 * Format the Environmental Section 
 * in SMF 98 subtype 1 (z/OS) records
 * <p>
 * This class uses the Smf2JsonCLI class to provide a command line 
 * interface to handle input and output specified by command line 
 * options and generate the JSON. 
 *
 */
public class EnvironmentSections 
{
    public static void main(String[] args) throws IOException                                   
    {
        Smf2JsonCLI cli = Smf2JsonCLI.create()
                .includeRecords(98,1)
                .description("Format SMF 98 Environment Section");
        
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
            
            // format flags as hex
            .exclude(EnvironmentalSection.class, "flagsByte1")
            .exclude(EnvironmentalSection.class, "flagsByte2")
            .exclude(EnvironmentalSection.class, "flagsByte3")
            .calculateEntry(EnvironmentalSection.class, "flagsByte1", x -> String.format("0x%02X", x.flagsByte1()))
            .calculateEntry(EnvironmentalSection.class, "flagsByte2", x -> String.format("0x%02X", x.flagsByte2()))
            .calculateEntry(EnvironmentalSection.class, "flagsByte3", x -> String.format("0x%02X", x.flagsByte3()))
            
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
            
            if (r98.environmentalSection() != null)
            {      
                CompositeEntry result = new CompositeEntry()
                        .add("smfid", r98.system())
                        .add("intervalStart", 
                                r98.identificationSection().smf98intervalStart()
                                    .atOffset(r98.contextSummarySection().cvtldto()))
                        .add("intervalEnd", 
                                r98.identificationSection().smf98intervalEnd()
                                    .atOffset(r98.contextSummarySection().cvtldto()))
                        .add(r98.identificationSection())
                        .add(r98.environmentalSection())
                        ;
    
                return Collections.singletonList(result);
            }
            else
            {
                return null;
            }
        } 
    }
}
