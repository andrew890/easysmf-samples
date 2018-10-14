package com.smfreports;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.*;
import com.blackhillsoftware.smf.smf70.*;

/**
 * List the jobs by jobname that used the most CPU time on each system 
 * in the 4 hours up to and including the top 5 4HRA MSU peaks.
 * MSU usage for each job name is also estimated using jobname CPU, total 
 * CPU and SMF70LAC value.
 * 
 * The report requires SMF 30 data from the 4 hours up to the 4HRA peak
 * otherwise results will be incorrect. 
 * 
 * We use SMF 70 subtype 1 for SMF70LAC values, and AMF 30 subtypes 2 and 3
 * for interval CPU usage.
 * 
 */

public class Peak4HRAJobs {
    public static void main(String[] args) throws IOException                                   
    {
        // Create nested Maps so we have a hierarchy of 
        // System -> Hour -> SMF70LAC
        Map< String, 
            Map< LocalDateTime, 
                HourlyLac > > systemHourLAC 
                    = new HashMap<String, Map<LocalDateTime, HourlyLac>>();
        
        // System -> Hour -> Jobname -> JobTotals
        Map< String, 
            Map< LocalDateTime, 
                Map< String, 
                    HourlyJobTotals > > > systemHourJobnameTotals 
                        = new HashMap<String, Map<LocalDateTime, Map<String, HourlyJobTotals>>>();
        
        // If we received no arguments, open DD INPUT
        // otherwise use the first argument as the file 
        // name to read.
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0]))                
                        .include(70,1)
                        .include(30,2)
                        .include(30,3))
        { 
            for (SmfRecord record : reader)                                                     
            {
                switch (record.recordType())
                {
                case 70:
                    Smf70Record r70 = new Smf70Record(record);
                    systemHourLAC
                        // computeIfAbsent creates a new entry if the key is not found,
                        // otherwise returns the existing entry
                        .computeIfAbsent(r70.system(), system -> new HashMap<LocalDateTime, HourlyLac>())
                        .computeIfAbsent(r70.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HourlyLac())
                        .add(r70); // Add the record to the HourlyLac entry for this System:Time
                    break;
                case 30:    
                        Smf30Record r30 = new Smf30Record(record);
                        systemHourJobnameTotals
                            // System
                            .computeIfAbsent(r30.system(), system -> new HashMap<LocalDateTime, Map<String, HourlyJobTotals>>())
                            // Hour
                            .computeIfAbsent(r30.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HashMap<String, HourlyJobTotals>())
                            // Job name
                            .computeIfAbsent(r30.identificationSection().smf30jnm(), 
                                    jobname -> new HourlyJobTotals(r30.identificationSection().smf30jbn()))                     
                            .add(r30); // Add the record to the HourlyJobTotals entry for this System:Time:Jobname
                    break;
                default:
                    break;
                }
            }
        }
        createReport(systemHourLAC, systemHourJobnameTotals);
    }

    private static void createReport(
            Map<String, Map<LocalDateTime, HourlyLac>> systemHourLAC,
            Map<String, Map<LocalDateTime, Map<String, HourlyJobTotals>>> systemHourJobnameTotals) 
    {
        systemHourLAC.entrySet().stream() // System names
            // Sort names
            .sorted((systemAinfo, systemBinfo) -> systemAinfo.getKey().compareTo(systemBinfo.getKey()))
            .forEachOrdered(systemInfo ->
            {
                String system = systemInfo.getKey();
                System.out.format("%n%n%s%n", system);
            
                systemInfo.getValue().entrySet().stream() // Information for each hour
                    .sorted((hourA,hourB) // Sort entries
                            // comparing the average LAC for the hour 
                            // reversed to sort descending 
                            -> Long.compare(hourB.getValue().hourAverage(), hourA.getValue().hourAverage()))
                    .limit(5) // take the first (top) 5 entries
                    .forEachOrdered(hourEntry -> // for each of the top hours
                    {
                        // write information
                        LocalDateTime hour = hourEntry.getKey();
                        long fourHourMSU = hourEntry.getValue().hourAverage();
                        System.out.println("");
                        System.out.println("    Hour                         4H MSU");
                    
                        System.out.format("    %10s %8s %15d%n", 
                                hour.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                hour.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                fourHourMSU);
                        // find and list top jobs by CPU time
                        reportFourHourTopJobs(hour, fourHourMSU, systemHourJobnameTotals.get(system));
                    });
            }
            );
    }

    private static void reportFourHourTopJobs(
            LocalDateTime hour,
            long msuvalue,
            Map<LocalDateTime, Map<String, HourlyJobTotals>> hourJobnameTotals) 
    {       
        System.out.println("");
        System.out.println("        Jobname       CPU%     Est. MSU");

        List<HourlyJobTotals> fourHourJobs = new ArrayList<>();
        
        for (int i = 0; i < 4; i++)
        {
            if (hourJobnameTotals.containsKey(hour.minusHours(i)))
            {
                fourHourJobs.addAll(hourJobnameTotals.get(hour.minusHours(i)).values());
            }
        }
                        
        double fourHourTotal = fourHourJobs.stream()
                .collect(Collectors.summingDouble(HourlyJobTotals::getCPTime));
        
        fourHourJobs.stream()
            .collect(
                Collectors.groupingBy(HourlyJobTotals::getJobname, 
                        Collectors.summingDouble(HourlyJobTotals::getCPTime)))
            .entrySet().stream()
            .sorted((jobATotal,jobBTotal) -> jobBTotal.getValue().compareTo(jobATotal.getValue()))
            .limit(5)
            .forEachOrdered(jobTotal ->                     
                System.out.format("        %-12s %4.1f%% %12.1f%n", 
                        jobTotal.getKey(), 
                        jobTotal.getValue() / Duration.ofHours(4).getSeconds() * 100,
                        jobTotal.getValue() / fourHourTotal * msuvalue));
    }

    private static class HourlyJobTotals
    {
        public HourlyJobTotals(String jobname)
        {
            Jobname = jobname;
        }
        public void add(Smf30Record r30)
        {
            ProcessorAccountingSection pacct = r30.processorAccountingSection();
            if (pacct != null)
            {
                cptime = cptime
                    + pacct.smf30cptSeconds()
                    + pacct.smf30cpsSeconds()
                    + pacct.smf30icuSeconds()
                    + pacct.smf30isbSeconds()
                    + pacct.smf30iipSeconds()
                    + pacct.smf30rctSeconds()
                    + pacct.smf30hptSeconds()
                    ;
            }
        }
        
        public double getCPTime()
        {
            return cptime;
        }
        
        public String getJobname()
        {
            return Jobname;
        }
        
        private double cptime = 0;
        private String Jobname;
    }
    
    private static class HourlyLac
    {
        // average is weighted by the number of samples in case of
        // short RMF intervals
        public void add(Smf70Record r70)
        {
            smf70lac += r70.cpuControlSection().smf70lac() * r70.productSection().smf70sam();
            smf70sam += r70.productSection().smf70sam();
        }
        
        public long hourAverage()
        {
            return smf70sam > 0 ? smf70lac / smf70sam : 0;
        }
        
        private long smf70lac = 0;
        private long smf70sam = 0;
    }
    
    
}
