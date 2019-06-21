import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

/**
 * Sample 5 shows A/B reporting, in this case before/after a specific date.
 * 
 * The sample reports CP time, zIIP time, zIIP on CP time, EXCP count, 
 * zIIP and zIIP on CP time as a percentage of total (normalized) CPU time,
 * and CPU milliseconds per I/O for each Program Name.
 * 
 * The A and B values for zIIP%, zIIP on CP% and CPU ms per I/O are compared and the 
 * difference is shown.
 * 
 * Statistics are collected in the same way as for sample4, except that we use 2 maps,
 * one for group A and one for B. 
 *
 */

public class sample5
{
    public static void main(String[] args) throws IOException
    {
        Map<String, ProgramStatistics> aPrograms = new HashMap<String, ProgramStatistics>();
        Map<String, ProgramStatistics> bPrograms = new HashMap<String, ProgramStatistics>();

        // If we received no arguments, open DD INPUT
        // otherwise use the argument as a file name.
        try (SmfRecordReader reader = args.length == 0 ? 
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {
        	// SMF 30 subtype 4 = Step End records
        	reader.include(30, 4);
        	
        	for (SmfRecord record : reader)
        	{
        		Smf30Record r30 = Smf30Record.from(record);
        		
        		// is this record part of group A or B?
        		Map<String, ProgramStatistics> target = isA(r30) ? aPrograms : bPrograms;
        	
        		// Find the program name and accumulate the data
                ProgramStatistics program = target
                	.computeIfAbsent(
                			r30.identificationSection().smf30pgm(), // program name
                			x -> new ProgramStatistics());
                program.accumulateData(r30);                 
        	}
        }
        writeReport(aPrograms, bPrograms);
    }

    /**
     * Test whether the record belongs to group A. In this case simply
     * based on the date of the record. 
     * @param r30 The SMF type 30 record
     * @return boolean Is the record part of group A. 
     */
    private static boolean isA(Smf30Record r30)
    {
    	final LocalDateTime boundary = LocalDateTime.of(2019, 05, 24, 0, 0);
    	return r30.smfDateTime().isBefore(boundary);   	
    }
    
    /**
     * Write the report
     * 
     * @param aPrograms
     *            The map of Program Names to Program Data for group A
     * @param bPrograms
     *            The map of Program Names to Program Data for group B
     */
    private static void writeReport(
    		Map<String, ProgramStatistics> aPrograms, 
    		Map<String, ProgramStatistics> bPrograms)
    {
    	// format strings for heading and detail lines.
    	String headerFormatString =  "%n%-8s %8s %14s %14s %14s %14s %14s %14s %14s%n";
    	String detailFormatString =  "%-8s %,8d %14s %14s %14s %,14d %13.1f%% %13.1f%% %14s%n";
    	String changeFormatString =  "%-77s %14s %14s %14s%n%n";

        // Headings
        System.out.format(headerFormatString, 
            "Program", "Count", "CP", "zIIP", "zIIP on CP",
            "EXCP", "zIIP%", "zIIP on CP%", "CPU ms/IO");

        // Take entries in Group A
        aPrograms.entrySet().stream()
        
            // sort by CP Time
            // reversing x and y in the comparison so sort is descending
            .sorted((x, y) -> Double.compare(y.getValue().cpTime, x.getValue().cpTime))
            
            // Ignore any entries where there is no Group B equivalent 
            .filter(aProgram -> bPrograms.containsKey(aProgram.getKey()))
            .limit(100) // take top 100
            .forEachOrdered(aProgram ->
            {
            	// Get A statistics and matching B statistics
                ProgramStatistics programAInfo = aProgram.getValue();
                ProgramStatistics programBInfo = bPrograms.get(aProgram.getKey());
                
                System.out.format("%-8s%n", 
                        aProgram.getKey()); // Program name
                
                // write Group A detail line
                System.out.format(detailFormatString, 
                    "A:",
                    programAInfo.count, 
                    hhhmmss(programAInfo.cpTime), 
                    hhhmmss(programAInfo.ziipTime),
                    hhhmmss(programAInfo.ziipOnCpTime),
                    programAInfo.excps, 
                    programAInfo.getZiipPct(), 
                    programAInfo.getZiipOnCpPct(), 
                    programAInfo.getCpuMsPerIO()
	            		.map(value -> String.format("%14.3f",value))
	            		.orElse("") );               
                
                // write Group B detail line
                System.out.format(detailFormatString, 
                    "B:",
                    programBInfo.count, 
                    hhhmmss(programBInfo.cpTime), 
                    hhhmmss(programBInfo.ziipTime),
                    hhhmmss(programBInfo.ziipOnCpTime),
                    programBInfo.excps, 
                    programBInfo.getZiipPct(), 
                    programBInfo.getZiipOnCpPct(), 
                    programBInfo.getCpuMsPerIO()
	            		.map(value -> String.format("%14.3f",value))
	            		.orElse("") );
                
                // write differences
                // Differences are type Optional<Double>. If values are zero
                // the percentage change is not useful so we don't report it.
                // If the Optional has a value, format the difference for output.
                // If not, leave the field blank.
                System.out.format(changeFormatString, 
                    "Change:",
                    programBInfo.getZiipPctChange(programAInfo)
                    		.map(value -> String.format("%+13.1f%%",value))
                    		.orElse(""),
                    programBInfo.getZiipOnCpPctChange(programAInfo)
                    		.map(value -> String.format("%+13.1f%%",value))
                    		.orElse(""),
                    programBInfo.getCpuMsPerIOChange(programAInfo)
                    		.map(value -> String.format("%+13.0f%%",value))
                    		.orElse("")
                		
                		);
            });
    }
    
    /**
     * A class to accumulate information about a program.
     */
    private static class ProgramStatistics
    {
        /**
         * Add information from a SMF 30 record.
         * 
         * @param r30
         *            The Smf30Record
         */
        public void accumulateData(Smf30Record r30)
        {
            // One step can have many SMF records so we might get called multiple times
            // for the same job step, but some of the SMF sections will occur only
            // once per step e.g. ProcessorAccountingSection.
        	
            if (r30.processorAccountingSection() != null)
            {
                count++; // pick a section that only occurs once and use to count job steps
                cpTime += r30.processorAccountingSection().smf30cptSeconds()
                    + r30.processorAccountingSection().smf30cpsSeconds();
                ziipTime += r30.processorAccountingSection().smf30TimeOnZiipSeconds();
                ziipOnCpTime += r30.processorAccountingSection().smf30TimeZiipOnCpSeconds();
                
                // According to the SMF manual the Processor Accounting section and 
                // Performance section can only occur in the first SMF record.
                // Assume a Performance section will always accompany the 
                // Processor Accounting section.
                normalizedZiipTime += 
                	r30.processorAccountingSection().smf30TimeOnZiipSeconds() 
                		* r30.performanceSection().smf30snf() / 256;
                
            }
            if (r30.ioActivitySection() != null)
            {
                excps += r30.ioActivitySection().smf30tex();
            }
        }
               
        /**
         * Calculate CPU time (CP time + normalized zIIP time) in milliseconds
         * per I/O (EXCP). If EXCP count = 0, return Optional.empty.
         * @return Optional<Double> CPU milliseconds per I/O, or Optional.empty 
         * if it cannot be calculated.
         */
        Optional<Double> getCpuMsPerIO()
        {
        	return excps > 0 ? 
        			Optional.of((cpTime + normalizedZiipTime) * 1000  / excps)
        			: Optional.empty();
        }
        
        /**
         * Calculate the percentage change in CPU time per I/O. 
         * If this or the previous value are zero,
         * no value will be calculated and Optional.empty will be returned.
         * 
         * @param prev the previous ProgramStatistics for comparison 
         * @return Optional<Double> percentage change, or Optional.empty
         *  if the value cannot be calculated
         */
        Optional<Double> getCpuMsPerIOChange(ProgramStatistics prev)
        {
        	Optional<Double> oldvalue = prev.getCpuMsPerIO();
           	Optional<Double> newvalue = this.getCpuMsPerIO();
           	
           	return (oldvalue.isPresent() 
           			&& newvalue.isPresent() 
           			&& oldvalue.get() > 0 
           			&& newvalue.get() > 0) ?         	
	        			Optional.of(((newvalue.get() / oldvalue.get()) - 1) * 100)
	        			: Optional.empty();
        }
        
        double getZiipPct()
        {
        	return (cpTime + normalizedZiipTime) > 0 ? normalizedZiipTime / (cpTime + normalizedZiipTime) * 100 : 0;
        }
        
        /**
         * Calculate the change in zIIP time as a percentage of total CPU time. 
         * If this or the previous value are zero,
         * no value will be calculated and Optional.empty will be returned.
         * 
         * @param prev the previous ProgramStatistics for comparison 
         * @return Optional<Double> percentage change, or Optional.empty
         *  if the value cannot be calculated
         */
        Optional<Double> getZiipPctChange(ProgramStatistics prev)
        {
        	return (this.getZiipPct() > 0 && prev.getZiipPct() > 0) ? 
        			Optional.of(this.getZiipPct() - prev.getZiipPct())
        			: Optional.empty();
        }
        
        double getZiipOnCpPct()
        {
        	return (cpTime + normalizedZiipTime) > 0 ? ziipOnCpTime / (cpTime + normalizedZiipTime) * 100 : 0;
        }
        
        /**
         * Calculate the change in zIIP on CP time as a percentage of total CPU time. 
         * If this or the previous value are zero,
         * no value will be calculated and Optional.empty will be returned.
         * 
         * @param prev the previous ProgramStatistics for comparison 
         * @return Optional<Double> percentage change, or Optional.empty
         *  if the value cannot be calculated
         */
        Optional<Double> getZiipOnCpPctChange(ProgramStatistics prev)
        {
        	return (this.getZiipOnCpPct() > 0 && prev.getZiipOnCpPct() > 0) ? 
        			Optional.of(this.getZiipOnCpPct() - prev.getZiipOnCpPct()) 
        			: Optional.empty();
        }
        
        int    count                 = 0;
        double cpTime                = 0;
        double ziipOnCpTime          = 0;
        double ziipTime              = 0;        
        double normalizedZiipTime    = 0;
        long   excps                 = 0;
    }   
    
    /**
     * Format seconds as hhh:mm:ss.hh Seconds value is reported
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