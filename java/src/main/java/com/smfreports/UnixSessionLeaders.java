package com.smfreports;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.*;

public class UnixSessionLeaders
{
    public static void main(String[] args) throws IOException
    {
        // A map of Job Names to JobData entries to collect information about that
        // group of jobs.

        Map<IdentificationSection, JobData> jobs = new HashMap<IdentificationSection, JobData>();

        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {
            for (SmfRecord r : reader.include(30, 5))
            {
                Smf30Record r30 = new Smf30Record(r);
                if (r30.unixProcessSections().size() > 0 
                    || r30.header().smf30opm() > 0)
                {
                    JobData job = jobs.computeIfAbsent(
                        r30.identificationSection(), 
                        x -> new JobData(r30));
                    job.add(r30);
                }
            }
        }
        
        List<JobData> sessionLeaders = 
            jobs.values().stream()
                .filter(process -> process.sessionLeader())
                .collect(Collectors.toList());
        writeReport(sessionLeaders);
    }

    /**
     * Write the report
     * 
     * @param jobs
     *            
     */
    private static void writeReport(List<JobData> jobs)
    {
        // Headings
        System.out.format("%n%-8s %-24s %-24s %-24s %-24s %-24s %n", 
            "Name", "Started", "Ended", "Service Class",
            "Report Class", "Service Group");

        jobs.stream()
            .sorted(Comparator.comparing(JobData::getJobName)
                .thenComparing(JobData::getStartTime))
            .forEachOrdered(jobname ->
            {
                System.out.format("%-8s %-24s %-24s %-24s %-24s %-24s %n", 
                    jobname.getJobName(),
                    jobname.getStartTime(),
                    jobname.getEndTime(),
                    jobname.serviceClass,
                    jobname.reportClass,
                    jobname.serviceGroup);
            });
    }

    /**
     * A class to accumulate information about a job.
     */
    private static class JobData
    {
        public JobData(Smf30Record record)
        {
            identificationsection = record.identificationSection();
            startTime = identificationsection.smf30std()
                .atTime(identificationsection.smf30sit());
            endTime = record.smfDateTime();
        }
        private IdentificationSection identificationsection;
        
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
            if (r30.performanceSection() != null)
            {
                serviceClass = r30.performanceSection().smf30scn();
                reportClass = r30.performanceSection().smf30rcn();
                serviceGroup = r30.performanceSection().smf30grn();
            }

            for (UnixProcessSection p : r30.unixProcessSections())
            {
                processes.add(new ProcessId(p));
            }
        }
        
        public boolean sessionLeader()
        {
            Optional<ProcessId> sessionleader = 
                processes.stream()
                    .filter(process -> process.sessionLeader())
                    .findFirst();
            return sessionleader.isPresent();
        }

        String getJobName()
        {
            return identificationsection.smf30jbn();
        }
        
        LocalDateTime getStartTime()
        {
            return startTime;
        }
        LocalDateTime getEndTime()
        {
            return endTime;
        }

        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        String serviceClass;
        String reportClass;
        String serviceGroup;
        
        List<ProcessId> processes = new ArrayList<>();
    }
    
    /**
     * A class to accumulate information about a job.
     */
    private static class ProcessId
    {
        ProcessId(UnixProcessSection process)
        {
            processId = process.smf30opi();
            sessionId = process.smf30osi();
        }
        
        public boolean sessionLeader()
        {
            return processId == sessionId;
        }
        
        long processId;
        long sessionId;
    }
    
    
}
