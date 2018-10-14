package com.smfreports;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.Smf30Record;

/**
 * Summarize Job statistics by job name
 *
 */
public class JobsByJobname
{
    public static void main(String[] args) throws IOException
    {
        // A map of Job Names to JobData entries to collect information about that
        // group of jobs.

        Map<String, JobData> jobs = new HashMap<String, JobData>();

        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {
            reader.include(30, 5) // SMF 30 subtype 5 = Job End records
                .stream().map(record -> new Smf30Record(record))
                // Optionally filter here, e.g. to include only jobs running in job class A:
                // .filter(r30 -> r30.identificationSection().smf30cl8().equals("A"))
                .forEach(r30 ->
                {
                    JobData job = jobs.computeIfAbsent(
                        r30.identificationSection().smf30jbn(), 
                        x -> new JobData());
                    job.add(r30);
                });

        }
        writeReport(jobs);
    }

    /**
     * Write the report
     * 
     * @param jobs
     *            The map of Job Names to Job Data
     */
    private static void writeReport(Map<String, JobData> jobs)
    {
        // Headings
        System.out.format("%n%-8s %6s %14s %14s %14s %14s %14s %14s %14s %14s%n", 
            "Name", "Count", "CPU", "zIIP",
            "Connect", "Excp", "Avg CPU", "Avg zIIP",
            "Avg Connect", "Avg Excp");

        jobs.entrySet().stream()
            // sort by CP Time
            // reversing a and b in the comparison so sort is descending
            .sorted((a, b) -> Double.compare(b.getValue().cpTime, a.getValue().cpTime))
            .limit(100) // take top 100
            .forEachOrdered(jobname ->
            {
                JobData jobinfo = jobname.getValue();
                // write detail line
                System.out.format("%-8s %,6d %14s %14s %14s %,14d %14s %14s %14s %,14d%n", 
                    jobname.getKey(),
                    jobinfo.count, 
                    hhhmmss(jobinfo.cpTime), 
                    hhhmmss(jobinfo.ziipTime),
                    hhhmmss(jobinfo.connectTime), 
                    jobinfo.excps, hhhmmss(jobinfo.cpTime / jobinfo.count),
                    hhhmmss(jobinfo.ziipTime / jobinfo.count), 
                    hhhmmss(jobinfo.connectTime / jobinfo.count),
                    jobinfo.excps / jobinfo.count);
            });
    }

    /**
     * A class to accumulate information about a group of jobs.
     */
    private static class JobData
    {
        /**
         * Add information from a SMF 30 record.
         * One job can have many SMF records so we might get called multiple times
         * for the same job, but some of the SMF sections will occur only
         * once per job e.g. ProcessorAccountingSection.
         * 
         * @param r30
         *            The Smf30Record
         */
        public void add(Smf30Record r30)
        {
            if (r30.processorAccountingSection() != null)
            {
                count++; // pick a section that only occurs once and use to count jobs
                cpTime += r30.processorAccountingSection().smf30cptSeconds()
                    + r30.processorAccountingSection().smf30cpsSeconds();
                ziipTime += r30.processorAccountingSection().smf30TimeOnZiipSeconds();
            }
            if (r30.ioActivitySection() != null)
            {
                excps += r30.ioActivitySection().smf30tex();
                connectTime += r30.ioActivitySection().smf30aicSeconds();
            }
        }

        int   count       = 0;
        double cpTime      = 0;
        double ziipTime    = 0;
        double connectTime = 0;
        long  excps       = 0;
    }

    /**
     * Format seconds as hhh:mm:ss. Seconds value is reported
     * to 2 decimal places.
     * 
     * @param totalseconds
     * @return The formatted value.
     */
    private static String hhhmmss(double totalseconds)
    {
        final int SECONDS_PER_MINUTE = 60;
        final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;

        int hours = (int) (totalseconds / SECONDS_PER_HOUR);
        int minutes = (int) ((totalseconds % SECONDS_PER_HOUR)) / SECONDS_PER_MINUTE;
        double seconds = totalseconds % SECONDS_PER_MINUTE;

        return String.format("%d:%02d:%05.2f", hours, minutes, seconds);
    }
    
}
