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
 * List the jobs by job name that used the most CPU time on each system 
 * in the 4 hours up to and including the top 5 4HRA MSU peaks.
 * MSU usage for each job name is also estimated by calculating the 
 * CPU time for the job name as a proportion of all CPU time seen for
 * those hours, and apportioning the SMF70LAC MSU value. This will not be 
 * totally accurate due to time not captured in type 30 records, and 
 * jobs that don't write type 30.2 and 30.3 records e.g. system tasks
 * that write 30.6.   
 * 
 * The report requires SMF 30 data from the 4 hours up to the 4HRA peak
 * otherwise results will be incorrect. 
 * 
 * The report uses SMF 70 subtype 1 for SMF70LAC values, and SMF 30 subtypes 
 * 2 and 3 for interval CPU usage.
 * 
 * Only 1 pass of the data is required, and data does not need to be in order.
 * 
 * The program gathers data using nested HashMaps. One set of Maps organizes
 * SMF70LAC by System and Hour. The second set of Maps organizes Job CPU 
 * totals by System, Hour and Job Name. The key for the hourly information is
 * the LocalDateTime truncated to the hour (retaining the date information).
 * 
 */

public class Peak4HRAJobs {
    public static void main(String[] args) throws IOException                                   
    {
        // Create nested Maps so we have a hierarchy of 
        // System -> Hour -> SMF70LAC
        Map< String, 
            Map< LocalDateTime, 
                HourlyLac > > 
        systemHourLAC 
                    = new HashMap<String, Map<LocalDateTime, HourlyLac>>();
        
        // System -> Hour -> Jobname -> Totals
        Map< String, 
            Map< LocalDateTime, 
                Map< String, 
                    JobnameTotals > > > 
        systemHourJobnameTotals 
                        = new HashMap<String, Map<LocalDateTime, Map<String, JobnameTotals>>>();
        
        // If we received no arguments, open DD INPUT.
        // Otherwise use the first argument as the file 
        // name to read.
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0])))
        {
        	reader
        		.include(70,1)
        		.include(30,2)
        		.include(30,3); 
        	
        	// Accumulate type 30 and type 70 data
            for (SmfRecord record : reader)                                                     
            {
                switch (record.recordType())
                {
                case 70:
                    Smf70Record r70 = new Smf70Record(record);
                    systemHourLAC
                        // computeIfAbsent creates a new entry if the key is not found,
                        // otherwise returns the existing entry
                    
                    	// Map of systems
                        .computeIfAbsent(r70.system(), system -> new HashMap<LocalDateTime, HourlyLac>())
                        // Nested map of hour -> hourly LAC for this system
                        .computeIfAbsent(r70.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HourlyLac())
                        .add(r70); // Add the record to the HourlyLac entry for this System:Time
                    break;
                case 30:    
                        Smf30Record r30 = new Smf30Record(record);
                        systemHourJobnameTotals
                            // System
                            .computeIfAbsent(r30.system(), system -> new HashMap<LocalDateTime, Map<String, JobnameTotals>>())
                            // Hour
                            .computeIfAbsent(r30.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HashMap<String, JobnameTotals>())
                            // Job name
                            .computeIfAbsent(r30.identificationSection().smf30jbn(), 
                                    jobname -> new JobnameTotals(r30.identificationSection().smf30jbn()))                     
                            .add(r30); // Add the record to the HourlyJobTotals entry for this System:Time:Jobname
                    break;
                default:
                    break;
                }
            }
        }
        writeReport(systemHourLAC, systemHourJobnameTotals);
    }

    private static void writeReport(
            Map<String, Map<LocalDateTime, HourlyLac>> systemHourLAC,
            Map<String, Map<LocalDateTime, Map<String, JobnameTotals>>> systemHourJobnameTotals) 
    {
        systemHourLAC.entrySet().stream() // Information from each system
            // Sort by system name
            .sorted((systemAinfo, systemBinfo) -> systemAinfo.getKey().compareTo(systemBinfo.getKey()))
            // For each system
            .forEachOrdered(systemInfo ->
            {
                System.out.format("%n%nSystem: %s%n", systemInfo.getKey()); // header
            
                systemInfo.getValue().entrySet().stream() // Information for each hour
	                // Sort entries
	                // comparing the average LAC for the hour 
	                // hourA,hourB reversed to sort descending
                    .sorted((hourA,hourB)  
                            -> Long.compare(hourB.getValue().hourAverageLAC(), hourA.getValue().hourAverageLAC()))
                    .limit(5) // take the first (top) 5 entries
                    .forEachOrdered(hourEntry -> // for each of the top hours
                    {
                        // write information about the hour
                        LocalDateTime hour = hourEntry.getKey();
                        long fourHourMSU = hourEntry.getValue().hourAverageLAC();

                        System.out.format("%n    %-19s %21s%n", "Hour","4H MSU");
                    
                        System.out.format("    %10s %8s %21d%n", 
                                hour.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                hour.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                fourHourMSU);
                        Map<LocalDateTime, Map<String, JobnameTotals>> hourJobname 
                        	= systemHourJobnameTotals.get(systemInfo.getKey());
                        // Get jobs for previous 4 hours
                        List<JobnameTotals> fourHourJobs = new ArrayList<>();       
                        for (int i = 0; i < 4; i++)
                        {
                            if (hourJobname.containsKey(hour.minusHours(i)))
                            {
                                fourHourJobs.addAll(hourJobname.get(hour.minusHours(i)).values());
                            }
                        }
                                       
                        // Calculate total CP time for all jobs during the 4 hours
                        double fourHourTotalCpTime = 
                        	fourHourJobs
                        		.stream()
                                .collect(Collectors.summingDouble(JobnameTotals::getCpTime));
                        
                        reportTopCpJobs(fourHourMSU, fourHourJobs, fourHourTotalCpTime);
                        reportTopZiipOnCpJobs(fourHourMSU, fourHourJobs, fourHourTotalCpTime);    
                    });
            }
            );
    }

    private static void reportTopJobsPrevious4Hours(
            LocalDateTime hour,
            long msuvalue,
            Map<LocalDateTime, Map<String, JobnameTotals>> hourJobname) 
    {       

    }

	private static void reportTopCpJobs(long msuvalue, List<JobnameTotals> fourHourJobs, double fourHourTotalCpTime) {
		// Heading
        System.out.format("%n        %-12s %11s %12s%n", 
        		"Jobname", "CPU%", "Est. MSU");
        
        // Build and print detail lines      
        fourHourJobs
        	.stream()
        	// Each job name might have entries from multiple hours
        	// Group by job name, and calculate sum of CP time for each job name 
            .collect(
                Collectors.groupingBy(JobnameTotals::getJobname, 
                        Collectors.summingDouble(JobnameTotals::getCpTime)))
            // process each job name
            .entrySet().stream()
            // sort job names by CP time, reversed to sort descending
            .sorted((jobATotal,jobBTotal) -> jobBTotal.getValue().compareTo(jobATotal.getValue()))
            // take top 5
            .limit(5) 
            // write detail lines
            .forEachOrdered(jobCpTime -> 
                System.out.format("        %-12s %10.1f%% %12.1f%n", 
                		// job name
                        jobCpTime.getKey(), 
                        // Average job CPU %
                        jobCpTime.getValue() / Duration.ofHours(4).getSeconds() * 100,
                        // Estimated MSU: 4 hour job CPU time / 4 hour all CPU time * 4 hour MSU 
                        jobCpTime.getValue() / fourHourTotalCpTime * msuvalue));
	}

	private static void reportTopZiipOnCpJobs(long msuvalue, List<JobnameTotals> fourHourJobs, double fourHourTotalCpTime) {
		// Heading
        System.out.format("%n%n        %-12s %11s %12s%n", 
        		"Jobname", "zIIP On CP%", "Est. MSU");
        
        // Build and print detail lines      
        fourHourJobs
        	.stream()
        	// Each job name might have entries from multiple hours
        	// Group by job name, and calculate sum of zIIP on CP time for each job name 
            .collect(
                Collectors.groupingBy(JobnameTotals::getJobname, 
                        Collectors.summingDouble(JobnameTotals::getZiipOnCpTime)))
            // process each job name
            .entrySet().stream()
            // sort job names by zIIP on CP time, reversed to sort descending
            .sorted((jobATotal,jobBTotal) -> jobBTotal.getValue().compareTo(jobATotal.getValue()))
            // take top 5
            .limit(5) 
            // write detail lines
            .forEachOrdered(jobCpTime -> 
                System.out.format("        %-12s %10.1f%% %12.1f%n", 
                		// job name
                        jobCpTime.getKey(), 
                        // Average job CPU %
                        jobCpTime.getValue() / Duration.ofHours(4).getSeconds() * 100,
                        // Estimated MSU: 4 hour zIIP on CP time / 4 hour all CPU time * 4 hour MSU 
                        jobCpTime.getValue() / fourHourTotalCpTime * msuvalue));
	}
    
    /**
     * A class to accumulate information for jobs with a particular jobname 
     *
     */
    private static class JobnameTotals
    {
        private double cpTime = 0;
        private double ziipOnCpTime = 0;
        private String jobname;
    	
    	/**
    	 * Constructor
    	 * @param jobname
    	 */
        public JobnameTotals(String jobname)
        {
            this.jobname = jobname;
        }
        
        /**
         * Accumulate information from a SMF record
         * @param r30 A type 30 record with information for this jobname
         */
        public void add(Smf30Record r30)
        {
        	if (!jobname.equals(r30.identificationSection().smf30jbn()))		
        	{
        		throw new IllegalArgumentException("Wrong job name: " + r30.identificationSection().smf30jbn());
        	}
            ProcessorAccountingSection pacct = r30.processorAccountingSection();
            if (pacct != null)
            {
                cpTime = cpTime
                    + pacct.smf30cptSeconds()
                    + pacct.smf30cpsSeconds()
                    + pacct.smf30icuSeconds()
                    + pacct.smf30isbSeconds()
                    + pacct.smf30iipSeconds()
                    + pacct.smf30rctSeconds()
                    + pacct.smf30hptSeconds()
                    ;
                ziipOnCpTime = ziipOnCpTime 
                	+ pacct.smf30TimeZiipOnCpSeconds();
            }
        }
        
        public double getCpTime()
        {
            return cpTime;
        }
        
        public double getZiipOnCpTime()
        {
            return ziipOnCpTime;
        }
        
        public String getJobname()
        {
            return jobname;
        }
    }
    
    /**
     * Keep a weighted average smf70lac.
     * The average is weighted by the number of samples (smf70sam)
     * to eliminate the effect of short intervals.
     *
     */
    private static class HourlyLac
    {
        private long smf70lac = 0;
        private long smf70sam = 0;
    	
    	/**
    	 * Accumulate information from a SMF 70 record.
    	 * @param r70 the SMF type 70 record 
    	 */
        public void add(Smf70Record r70)
        {
            smf70lac += r70.cpuControlSection().smf70lac() * r70.productSection().smf70sam();
            smf70sam += r70.productSection().smf70sam();
        }
        
        /**
         * Get the weighted average LAC
         * @return weighted average, or 0 if we have no samples
         */
        public long hourAverageLAC()
        {
            return smf70sam > 0 ? smf70lac / smf70sam : 0;
        }
    }
    
    
}
