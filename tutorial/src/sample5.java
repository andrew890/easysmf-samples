import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import static java.util.Comparator.comparing;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.*;

/**
 * This sample shows how to access sections where a section may be repeated, 
 * e.g. EXCP sections.
 * The sections are returned in a List<T>
 * The program lists each job/step/program that includes a STEPLIB.
 * Each job/step number/step name/program name combination is
 * listed only once.
 * Jobs where the Job name begins with the userid are excluded.
 * 
 * The report is designed to demonstrate the use of lists of  
 * sections, it is not meant to suggest that any action needs
 * to be taken for jobs with a STEPLIB.
 *
 */

public class sample5 
{
    public static void main(String[] args) throws IOException                                   
    {                
        // Use a Set to store job-step-program combinations. New items can
        // only be added if there is not already an equal() entry in the 
        // set - thus eliminating duplicates.
        // Specifically we are using a HashSet which uses a hash table to 
        // store entries and check for duplicates
        Set<JobStepProgram> jobStepPrograms = new HashSet<JobStepProgram>();
        
        // If we received no arguments, open DD INPUT
        // otherwise use the first argument as the file 
        // name to read.
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0])) 
        {
            reader.include(30,4); // SMF type 30 subtype 4 : Step End records
            
            // read and process records
            for (SmfRecord record : reader)
            {
                // create a type 30 record 
                Smf30Record r30 = Smf30Record.from(record);
                
                // ignore jobs where the job name starts with the userid
                if (!r30.identificationSection().smf30jbn()
                        .startsWith(r30.identificationSection().smf30rud()))
                {            
                    // Loop through the EXCP sections. Some records will have
                    // no EXCP sections - in that case the list is empty and
                    // we go through the loop zero times.
                    for (ExcpSection excp : r30.excpSections())
                    {
                        if (excp.smf30ddn().equals("STEPLIB"))
                        {
                            // add() returns false if there is a duplicate entry - 
                            // we ignore that because we don't need to know.
                            jobStepPrograms.add(new JobStepProgram(r30));    
                        }
                    }
                }
            }                                                                           
        }
        
        // write headings
        System.out.format("%-10s %6s  %-10s %-10s%n",
                "Job Name", "Step #", "Step Name", "Program");
        
        // Stream and sort entries, and write the details. 
        jobStepPrograms.stream()
            .sorted(
                    comparing(JobStepProgram::getJobName)
                    .thenComparing(JobStepProgram::getStepNumber)
                    .thenComparing(JobStepProgram::getStepName)
                    .thenComparing(JobStepProgram::getProgramName))
            .forEachOrdered( entry ->
                    System.out.format("%-10s %6d  %-10s %-10s%n",
                    entry.getJobName(),
                    entry.getStepNumber(),
                    entry.getStepName(),
                    entry.getProgramName()));
        
        
        System.out.println("Done");
    }
    
    /**
     *
     * A class to represent a job/step number/step name/program name
     * combination. This class provides hashCode() and equals() 
     * implementations to allow recognition of duplicates.
     *
     */
    private static class JobStepProgram
    {            
        private String jobname;
        private int stepnumber;
        private String stepname;
        private String programname;        
        
        public JobStepProgram(Smf30Record r30)
        {
            jobname = r30.identificationSection().smf30jbn();
            stepname = r30.identificationSection().smf30stm();
            programname = r30.identificationSection().smf30pgm();
            stepnumber = r30.identificationSection().smf30stn();
        }

        /*
         * To avoid bugs when using e.g. hash tables, ALWAYS override
         * hashCode and equals together.
         * This hashCode and equals code was generated by Eclipse 
         * based on the fields in the class
         */
        
        @Override
        public int hashCode() {
            return Objects.hash(jobname, programname, stepname, stepnumber);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            JobStepProgram other = (JobStepProgram) obj;
            return Objects.equals(jobname, other.jobname)
                    && Objects.equals(programname, other.programname) 
                    && Objects.equals(stepname, other.stepname)
                    && stepnumber == other.stepnumber;
        }
        
        /*
         * 
         * Methods to access the fields - simplifies the syntax
         * when sorting compared to accessing the fields directly.
         *   
         */

        String getJobName() {
            return jobname;
        }

        int getStepNumber() {
            return stepnumber;
        }

        String getStepName() {
            return stepname;
        }

        String getProgramName() {
            return programname;
        }
    }  
}
