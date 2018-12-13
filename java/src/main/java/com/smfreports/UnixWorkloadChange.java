package com.smfreports;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.*;

/**
 * 
 * UnixWorkloadChange - search for work that changes WLM service class etc. 
 * 
 * Unix work often creates multiple processes. On z/OS these can run in 
 * a different address space to the original job. These additional address 
 * spaces may be classified into different workloads by WLM.
 * 
 * This program searches for Unix work where the WLM workload classification
 * is different to the parent job. To simplify the process, instead of following
 * the chain of parent processes to find the owning job, it uses the session 
 * ID to find the session leader process. This is most likely to be the job that 
 * originated the group of processes, and is probably the job that has the 
 * desired service/report class etc.
 * 
 * A type 30 record can contain information from multiple processes in the 
 * same address space. Additionally, process information can be split across
 * multiple type 30 records. We assume the first process section contains the 
 * most relevant session information for finding the parent address space. In  
 * case records are out of order, we keep a number indicating the order of the   
 * section relative to other sections in this and other records. For each 
 * process section we subtract the number of sections in following records 
 * from the index of the section in this record, and the lowest number section
 * should be the first section across all records. 
 *
 */
public class UnixWorkloadChange
{
    public static void main(String[] args) throws IOException
    {

        /*
         * Use the identification section as a key to accumulate multiple records from
         * the same job
         */
        Map<IdentificationSection, JobData> jobs = new HashMap<IdentificationSection, JobData>();

        /*
         * If we received no arguments, open DD INPUT otherwise use the argument as a
         * file name.
         */
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {
            for (SmfRecord r : reader.include(30, 5))
            {
                Smf30Record r30 = new Smf30Record(r);
                
                // only collect jobs where we have unix process sections in this or a later record
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
        
        /*
         * Map session leaders by session id. Process ids (and therefore session ids)
         * can be reused, so for each session id so we use a list, and later search for
         * the session leader in the list based on system, start and end time. Collect
         * jobs that are not session leaders in a separate list.
         */
        
        Map<Long, List<JobData>> sessionLeaders = new HashMap<>();
        List<JobData> others = new ArrayList<>();
        for (JobData job : jobs.values())
        {
            if (job.isSessionLeader())
            {
                sessionLeaders
                    .computeIfAbsent(job.getSessionId(),
                        x -> new ArrayList<JobData>())
                    .add(job);
            }
            else
            {
                others.add(job);
            }   
        }
        
        /*
         * For all non session leaders, attempt to find the session leader. If the
         * service class doesn't match, add it to the list of mismatches.
         */
        
        List<JobData> mismatches = new ArrayList<>();
        for (JobData job : others)
        {
            List<JobData> possibleLeaders = sessionLeaders.get(job.sessionId);
            if (possibleLeaders != null)
            {
                /*
                 * We are looking for a job from the same system which was running when this job
                 * started, i.e. this job start time is between the start and end times of the
                 * other job. We already know the process ID is the same as this job's session
                 * id. It might be possible to have more than one match, but in that case we
                 * can't tell the difference so just settle for the first.
                 */
                
                JobData leader = possibleLeaders.stream()
                    .filter(maybeLeader -> maybeLeader.getSystem().equals(job.getSystem())
                        && (maybeLeader.startTime.isBefore(job.startTime) || maybeLeader.startTime.equals(job.startTime))
                        && (maybeLeader.endTime.isAfter(job.startTime) || maybeLeader.endTime.equals(job.startTime)))
                    .findFirst()
                    .orElse(null);
                
                if (leader != null
                    && job.getServiceClass() != leader.getServiceClass())
                {
                    job.setSessionLeader(leader);
                    leader.addChildMismatch(job);
                    mismatches.add(job);
                }                               
            }
        }
        
        Map<String, List<JobData>> mismatchesByServiceClass = 
            mismatches.stream()
                .filter(x -> !x.serviceClass.equals(x.getSessionServiceClass()))
                .collect(Collectors.groupingBy(job -> job.getSessionServiceClass()));
        
        List<String> serviceClassList = mismatchesByServiceClass.keySet().stream()
            .sorted()
            .collect(Collectors.toList());
        
        // for each service classes used by session leader jobs 
        for (String serviceClass : serviceClassList)
        {
            System.out.format("%s%n", serviceClass);
            
            // group jobs by session leader job names
            Map<String, List<JobData>> bySessionLeaderName = mismatchesByServiceClass.get(serviceClass).stream()
                 .collect(Collectors.groupingBy(JobData::getSessionLeaderName));
            
            List<String> sessionLeaderNames = bySessionLeaderName.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
            
            // for each session leader job name
            for (String sessionLeaderName : sessionLeaderNames)
            {
                System.out.format("%-10s        %-10s%n", sessionLeaderName, serviceClass);
                
                // group by job name. Typically JOBNAME1, JOBNAME2 etc.
                Map<String, List<JobData>> byJobName = bySessionLeaderName.get(sessionLeaderName).stream()
                     .collect(Collectors.groupingBy(JobData::getJobName));
                
                List<String> jobNames = byJobName.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());
                
                // for each job name
                for (String jobName : jobNames)
                {
                    /* 
                     * Jobs with the same name might have different service classes, e.g. 
                     * if the WLM policy changes, so group one more time before summarizing 
                     * by jobnmae/serviceclass
                     * */
                    
                    Map<String, List<JobData>> byJobServiceClass = byJobName.get(jobName).stream()
                        .collect(Collectors.groupingBy(JobData::getServiceClass));
                   
                    List<String> jobServiceClasses = byJobServiceClass.keySet().stream()
                       .sorted()
                       .collect(Collectors.toList());
                    
                    // for each service class for this job name
                    for (String jobServiceClass : jobServiceClasses)
                    {
                        System.out.format("        %-10s%-10s%n", jobName, jobServiceClass);
                        
                    }
                }
            }
            
        }

    }

    /**
     * A class to keep information about a job.
     */
    private static class JobData
    {
        private String system;
        private String jobname;
        private String serviceClass;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        private Long sessionId;
        private long sessionOpm = 0;
        
        private boolean isSessionLeader = false;       
        private JobData sessionLeader; 
        private List<JobData> mismatchChildren = new ArrayList<>();
        
        public JobData(Smf30Record record)
        {
            system = record.system();
            jobname = record.identificationSection().smf30jbn();
            startTime = record.identificationSection().smf30std()
                .atTime(record.identificationSection().smf30sit());
            endTime = record.smfDateTime();
        }
        
        /**
         * Add information from a SMF 30 record.
         * One job can have many SMF records so we might get called multiple times
         * for the same job, but some of the SMF sections will occur only
         * once per job e.g. PerformanceSection.
         * 
         * @param r30
         *            The Smf30Record
         */
        public void add(Smf30Record r30)
        {
            if (r30.performanceSection() != null)
            {
                serviceClass = r30.performanceSection().smf30scn();
            }

            if (r30.unixProcessSections().size() > 0
                && (sessionId != null || r30.header().smf30opm() > sessionOpm))
            {
                sessionId = r30.unixProcessSections().get(0).smf30osi();
                sessionOpm = r30.header().smf30opm();            
            }
            
            if (!isSessionLeader) // if we haven't already determined this
            {
                isSessionLeader = r30.unixProcessSections().stream()
                    .filter(process -> process.smf30opi() == process.smf30osi())
                    .findFirst()
                    .isPresent();
            }
        }
        
        public void addChildMismatch(JobData child)
        {
            mismatchChildren.add(child);
        }
        
        public Long getSessionId() { return sessionId; }
        public boolean isSessionLeader() { return isSessionLeader; }
        public List<JobData> getMismatchChildren() { return mismatchChildren; }
        public String getServiceClass() { return serviceClass; }
        public String getSystem() { return system; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getJobName() { return jobname; }
        public String getSessionLeaderName() { return sessionLeader.getJobName(); }
        public String getSessionServiceClass() { return sessionLeader.getServiceClass(); }
        public LocalDateTime getStartTime() { return startTime; }
        
        public JobData getSessionLeader() { return sessionLeader; }
        public void setSessionLeader(JobData sessionLeader) { this.sessionLeader = sessionLeader; }

    }  
}
