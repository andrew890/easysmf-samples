package com.blackhillsoftware.samples;

import java.io.IOException;
import java.time.*;
import java.util.*;

import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.Utils;
import com.blackhillsoftware.smf.smf30.ProcessorAccountingSection;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class PrimeShiftTopJobs
{
    public static void main(String[] args) throws IOException
    {

        // We need a Jobname->JobData map for each day of the week.           
        Map<DayOfWeek, HashMap<String, JobData>> jobsByDay = new HashMap<DayOfWeek, HashMap<String, JobData>>();

        // Read and process the data

        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0])) 
        {  
            reader
                .include(30, 2)
                .include(30, 3)
                .stream()
                .filter(record -> 
                     record.smfDate().getDayOfWeek().getValue() >= DayOfWeek.MONDAY.getValue()
                     && record.smfDate().getDayOfWeek().getValue()  <= DayOfWeek.FRIDAY.getValue()
                     && record.smfTime().isAfter(LocalTime.of(8, 30))
                     && record.smfTime().isBefore(LocalTime.of(17, 30)))
                .map(record -> new Smf30Record(record))
                .forEach(r30 ->
                {
                    if((r30.processorAccountingSection()) != null)
                    {
                        jobsByDay
                            .computeIfAbsent(r30.smfDate().getDayOfWeek(), day -> new HashMap<>())
                            .computeIfAbsent(r30.identificationSection().smf30jbn(), job -> new JobData())
                            .add(r30.processorAccountingSection());                        
                    }
                });
        
        }

        writeReport(jobsByDay);
    }

    private static void writeReport(Map<DayOfWeek, HashMap<String, JobData>> dailyJobs)
    {
        dailyJobs.entrySet().stream()
           .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
           .forEachOrdered(day -> 
           {
               // calculate total CPU for the day
               Duration totalCp = day.getValue().entrySet().stream()
                       .map(job -> job.getValue().cpTime)
                       .reduce(Duration.ZERO, (x, y) -> x.plus(y));
               // Headings
               System.out.format("%n%s%n", day.getKey().toString());
               System.out.format("%-8s %11s %5s %11s %11s %n", "Name", "CPU",
                       "CPU%", "zIIP", "zAAP");
               
               day.getValue().entrySet().stream()
                   .sorted((x, y) -> y.getValue().cpTime.compareTo(x.getValue().cpTime))
                   .limit(10)
                   .forEach(entry ->
                       {
                           JobData jobinfo = entry.getValue();    
                           // write detail line     
                           System.out.format(
                                   "%-8s %11s %4.0f%% %11s %11s %n",
                                   entry.getKey(), // key is jobname
                                   hhhmmss(jobinfo.cpTime),
                                   Utils.divideDurations(jobinfo.cpTime, totalCp) * 100,
                                   hhhmmss(jobinfo.ziipTime),
                                   hhhmmss(jobinfo.zaapTime));      
                       });
               
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
            cpTime = cpTime.plus(proc.smf30cpt()).plus(proc.smf30cps());
            zaapTime = zaapTime.plus(proc.smf30TimeOnIfa());
            ziipTime = ziipTime.plus(proc.smf30TimeOnZiip());
        }

        Duration cpTime = Duration.ZERO;
        Duration ziipTime = Duration.ZERO;
        Duration zaapTime = Duration.ZERO;
    }

}
