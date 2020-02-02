package com.smfreports.type30;

import java.io.IOException;
import java.time.*;
import java.util.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.IoActivitySection;
import com.blackhillsoftware.smf.smf30.ProcessorAccountingSection;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class JobnameStatistics
{
    public static void main(String[] args) throws IOException
    {
    	
        if (args.length < 1)
        {
            System.out.println("Usage: JobnameStatistics <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }

        Map<String, JobData> jobs = new HashMap<String, JobData>();

        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0])) 
        {  
            reader
                .include(30, 5)
                .stream()
                .map(record -> new Smf30Record(record))
                .filter(r30 -> r30.header().smf30con() != 0 || r30.header().smf30uon() != 0)
                .filter(r30 -> r30.identificationSection().smf30cl8().equals("A"))
                .forEach(r30 ->
                {
                    JobData job = jobs.computeIfAbsent(r30.identificationSection().smf30jbn(), x -> new JobData());
                    job.add(Utils.oneOrNull(r30.processorAccountingSections()));    
                    job.add(Utils.oneOrNull(r30.ioActivitySections()));          
                });
        
        }

        writeReport(jobs);
    }

    private static void writeReport(Map<String, JobData> jobs)
    {
        // Headings
        System.out.format("%n%-8s %5s %11s %11s %11s %11s %11s %11s %11s %11s %11s %11s%n", 
                "Name", "Count", 
                "CPU", "zIIP", "zAAP", "Connect", "Excp",
                "Avg CPU", "Avg zIIP", "Avg zAAP", "Avg Connect", "Avg Excp");
        jobs.entrySet().stream()
           .sorted((a, b) -> b.getValue().cpTime.compareTo(a.getValue().cpTime))
           .forEachOrdered(jobname -> 
           {
               JobData jobinfo = jobname.getValue();    
               // write detail line     
               System.out.format(
                       "%-8s %5d %11s %11s %11s %11s %11d %11s %11s %11s %11s %11d%n",
                       jobname.getKey(), 
                       jobinfo.count,
                       hhhmmss(jobinfo.cpTime),
                       hhhmmss(jobinfo.ziipTime),
                       hhhmmss(jobinfo.zaapTime),
                       hhhmmss(jobinfo.connectTime),
                       jobinfo.excps,
                       hhhmmss(jobinfo.cpTime.dividedBy(jobinfo.count)),
                       hhhmmss(jobinfo.ziipTime.dividedBy(jobinfo.count)),
                       hhhmmss(jobinfo.zaapTime.dividedBy(jobinfo.count)),
                       hhhmmss(jobinfo.connectTime.dividedBy(jobinfo.count)),
                       jobinfo.excps/jobinfo.count);                     
           });
    }

    private static String hhhmmss(Duration dur)
    {
        long hours = dur.toHours();
        long minutes = dur.minus(Duration.ofHours(hours)).toMinutes();
        long seconds = dur.minus(Duration.ofHours(hours))
                .minus(Duration.ofMinutes(minutes)).toMillis() / 1000;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    private static class JobData
    {
        public void add(ProcessorAccountingSection proc)
        {
            if (proc != null)
            {
                count++;
                cpTime = cpTime.plus(proc.smf30cpt()).plus(proc.smf30cps());
                zaapTime = zaapTime.plus(proc.smf30TimeOnIfa());
                ziipTime = ziipTime.plus(proc.smf30TimeOnZiip());
            }
        }
        
        public void add(IoActivitySection ioact)
        {
            if (ioact != null)
            {
                excps += ioact.smf30tex();
                connectTime = connectTime.plus(ioact.smf30aic());
            }
        }

        int count = 0;
        Duration cpTime = Duration.ZERO;
        Duration ziipTime = Duration.ZERO;
        Duration zaapTime = Duration.ZERO;
        
        Duration connectTime = Duration.ZERO;
        long excps = 0;
    }

}
