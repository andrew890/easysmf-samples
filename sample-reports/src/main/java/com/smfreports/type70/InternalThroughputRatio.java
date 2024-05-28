package com.smfreports.type70;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf70.*;
import com.blackhillsoftware.smf.smf70.subtype1.*;

/**
 * This program calculates ITR values for different system and machine 
 * combinations from SMF type 70 records, and produces a table of ITRR 
 * values for each combination.
 * 
 * It is implemented as a single file Java program so it can be run
 * from the command line under Java 11+ without a separate compilation step.
 * This is stretching the friendship a bit due to the size, however it
 * means that the include/exclude lists and shift definitions can be 
 * conveniently edited as required between runs.
 * 
 * To run the program:
 * - edit the includeSmfid and excludeSmfid if required.
 * - edit the shift definitions if you want to report multiple shifts, or
 *   just assign all data to one shift.
 * - run the program from the command line:
 *   java -cp '/path/to/easysmf/*.jar' InternalThroughputRatio.java input-file-1 [input-file-2 ...]
 *
 */
public class InternalThroughputRatio 
{
    public static void main(String[] args) throws IOException 
    {
        if (args.length < 1)
        {
            System.out.println("Usage: InternalThroughputRatio <input-name1> [<input-name2> ...]");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // read and process the input data
        List<IntervalData> intervals = processInput(args);
        
        // write the reports
        writeReport(intervals);
    }
    
    /**
     * SMFIDs for the systems to include. All systems will be included if
     * empty.
     */
    private static final List<String> includeSmfid = Arrays.asList(
            //"SYSA",
            //"SYSB"
            );
    
    /**
     * SMFIDs to be excluded. Overrides include entries.
     */
    private static final List<String> excludeSmfid = Arrays.asList(
            //"SYSC",
            //"SYSD"
            );    
    
    /**
     * Shifts for grouping the data. Add or remove shifts and 
     * update the selection logic as required.
     */
    private static enum Shift
    {
        PRIME,
        AFTER_HOURS,
        WEEKEND;
        
        private static final LocalTime primeStart = LocalTime.of(9, 00);
        private static final LocalTime primeEnd = LocalTime.of(17, 00);
        private static final List<DayOfWeek> weekend = Arrays.asList(
                DayOfWeek.SATURDAY, 
                DayOfWeek.SUNDAY
        );
        
        /**
         * Get a shift for a time value
         * @param time the time value
         * @return the Shift containing the time
         */
        public static Shift from(LocalDateTime time)
        {
            if (weekend.contains(time.getDayOfWeek()))     
            {
                return Shift.WEEKEND;
            }
            else if (time.toLocalTime().isAfter(primeStart) 
                    && time.toLocalTime().isBefore(primeEnd))
            {
                return Shift.PRIME;
            }
            else
            {
                return Shift.AFTER_HOURS;
            }
        }
    }

    // Useful constant for conversion to seconds
    private static final long NANOSPERSECOND = Duration.ofSeconds(1).toNanos(); 


    /**
     * Process the input data
     * @param args the arguments passed to the program - a list of files/datasets/ddnames
     * @return the list of IntervalData generated from the input data
     * @throws IOException
     * @throws FileNotFoundException
     */
    private static List<IntervalData> processInput(String[] args)
            throws IOException, FileNotFoundException 
    {
        // Type 70 records can be broken into multiple records.
        // We need to combine the parts to produce an accurate view
        // of all LPARs.
        // So we keep intervals in a Map so we can find previous records from
        // the same system/interval.
        
        // Create nested maps by System and Interval end date time to find an existing interval
        // System Name -> Interval time -> Interval Data
        Map<String, Map<LocalDateTime, IntervalData>> intervals = new HashMap<>();
        for (String arg : args)
        {
            try (SmfRecordReader reader = SmfRecordReader.fromName(arg)
                    .include(70, 1)) 
            {
                for (SmfRecord record : reader)
                {
                    // if we are including this system
                    if ((includeSmfid.isEmpty() || includeSmfid.contains(record.system()))
                            && !excludeSmfid.contains(record.system()))
                    {
                        Smf70Record r70 = Smf70Record.from(record);
 
                        intervals
                            // find the system name or create new
                            .computeIfAbsent(r70.productSection().smf70snm(), 
                                    system -> new HashMap<>())
                            // find the interval end time, or create new interval 
                            .computeIfAbsent(
                                    r70.productSection().smf70dat()
                                        .atTime(r70.productSection().smf70ist())
                                        .plus(r70.productSection().smf70int()), 
                                    interval -> new IntervalData(r70))
                            // add the CPU and partition data to the interval
                            .addCpuData(r70)
                            .addPartitionData(r70);
                    }
                }
            }
        }

        // combine all the intervals from the maps into a list
        List<IntervalData> result = new ArrayList<>();
        for (Map<LocalDateTime, IntervalData> systemEntry : intervals.values())
        {
            result.addAll(systemEntry.values());
        }
        return result;                
    }

    /**
     * Write the reports from the data
     * @param intervals the list of IntervalData generated from the input data
     */
    private static void writeReport(List<IntervalData> intervals) 
    {
        // first group the intervals by shift
        // we subtract half the interval duration from the end time  
        // to use the middle time to find the shift it best belongs to
        Map<Shift, List<IntervalData>> dataByShift = 
            intervals.stream()
                .collect(Collectors.groupingBy(entry -> 
                    Shift.from(entry.intervalEnd.minus(entry.intervalLength.dividedBy(2)))));
        
        // process each shift
        dataByShift.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(shift ->
            {   
                // first group data by system name+CPC 
                Map<String, List<IntervalData>> groupedBySystemCpc = 
                        shift.getValue().stream()
                            .collect(Collectors.groupingBy(entry -> entry.systemCpc));
                
                // then create a new map with the cumulative data for the groups
                Map<String, CumulativeData> cumulativeData = groupedBySystemCpc.entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey(), 
                            entry -> new CumulativeData(entry.getValue())));

                // Write Shift header
                System.out.format("%nShift: %s%n%n", shift.getKey());        
                
                // header for system/CPC summary 
                System.out.format("%20s %5s %10s %10s %10s %10s %10s %11s %10s %11s%n", 
                        "System/CPC",
                        "GCP#",
                        "Int/s",
                        "IO/s",
                        "Int/s/CP",
                        "IO/s/CP",
                        "LPAR CP",
                        "LPAR CP%",
                        "All CP",
                        "All CP%");
                
                // get the list of names to process from the keys in the Map
                List<String> systemCpcNames = cumulativeData.keySet().stream()
                        .sorted()
                        .collect(Collectors.toList());
                
                // write a summary for each system/CPC
                for (String name: systemCpcNames)
                {
                    CumulativeData totals = cumulativeData.get(name);
                    System.out.format("%20s %5.0f %10d %10d %10d %10d %10.1f %10.1f%% %10.1f %10.1f%%%n", 
                            name,
                            totals.avgCps(),
                            totals.getIoInterruptsPerSecond(),
                            totals.getIosPerSecond(),
                            totals.getIoInterruptsPerSecondPerCp(),
                            totals.getIosPerSecondPerCp(),
                            totals.lparBusyCps(),
                            totals.lparCpuPct(),
                            totals.allBusyCps(),
                            totals.allCpuPct());   
                }   

                // generate tables of the ITRR values, based on different fields
                
                System.out.format("%n  ITRR: LPAR, I/O Interrupts per second%n%n");
                ItrrTable(cumulativeData, systemCpcNames, CumulativeData::getIoInterruptsPerSecond);

                System.out.format("%n  ITRR: LPAR, I/Os per second%n%n");
                ItrrTable(cumulativeData, systemCpcNames, CumulativeData::getIosPerSecond);
                
                System.out.format("%n  ITRR: CP, I/O Interrupts per second%n%n");
                ItrrTable(cumulativeData, systemCpcNames, CumulativeData::getIoInterruptsPerSecondPerCp);

                System.out.format("%n  ITRR: CP, I/Os per second%n%n");
                ItrrTable(cumulativeData, systemCpcNames, CumulativeData::getIosPerSecondPerCp);
            });
    }

    /**
     * Generate a table of ITRR data. The specific method used to get the value for the calculation
     * is passed as a parameter.
     * @param cumulativeDataBySystemCpc the Cumulative data for each system/cpc
     * @param systemCpcNames the system/cpc names to process (the keys in the Map)
     * @param getValue a method to get the value used in the ratio calculation
     */
    private static void ItrrTable(Map<String, CumulativeData> cumulativeDataBySystemCpc,
            List<String> systemCpcNames,
            ToLongFunction<CumulativeData> getValue)
    {        
        System.out.format("%20s", ""); // empty first column heading
        for (String column : systemCpcNames) // column headings
        {
            System.out.format("%20s", column);
        }
        
        // Iterate the list of names for both rows and columns, to compare 
        // each system with each other system.
        // We get a diagonal of 1.0 values where a system is compared with
        // itself.
        for (String row : systemCpcNames)
        {
            System.out.format("%n%20s", row);
            for (String column : systemCpcNames)
            {
                // divide row vs column values to get ratio
                System.out.format("%20.2f", 
                        (double)getValue.applyAsLong(cumulativeDataBySystemCpc.get(column))
                            /(double)getValue.applyAsLong(cumulativeDataBySystemCpc.get(row)));  
            }
        }
        System.out.println();
    }   
    
    /**
     * A class to collect the data from a system for an interval. 
     */
    private static class IntervalData
    {
        public IntervalData(Smf70Record r70)
        {
            // format the system and CPC information, e.g. "SYSA 3931-401"
            systemCpc = String.format("%s %04X-%s",
                    r70.productSection().smf70snm(), 
                    r70.cpuControlSection().smf70mod(), 
                    r70.cpuControlSection().smf70mdl()
                    );

            intervalLength = r70.productSection().smf70int();
            
            // calculate the interval end from start date and time plus
            // interval length
            intervalEnd = r70.productSection().smf70dat()
                    .atTime(r70.productSection().smf70ist())
                    .plus(intervalLength);
            
            
            // find the GCP count for the interval by searching
            // CPU Identification sections for the section with
            // value "CP"
            cpCount = r70.cpuIdentificationSections()
                        .stream()
                        .filter(id -> id.smf70cin().equals("CP"))
                        .findFirst()
                        .map(idSection -> cpCount = idSection.smf70ctn())
                        .orElseThrow(() -> new RuntimeException("Could not find CP count, system: " + systemCpc));
        }
        
        private String systemCpc;
        private LocalDateTime intervalEnd;        
        private int cpCount;
        private Duration intervalLength;
        private long ioInterrupts = 0;
        private long io = 0;
        private double myEdt = 0;
        private double allEdt = 0;
        
        private double intervalSeconds() {
            return ((double)intervalLength.toNanos()) / NANOSPERSECOND;
        }
        
        // Add I/O interrupt and I/O count information from the
        // CPU data sections
        // Assumption: there are only CPU data sections for the partition
        // writing the record.
        public IntervalData addCpuData(Smf70Record r70)
        {
            for (CpuDataSection cpudata : r70.cpuDataSections())
            {
                if (cpudata.smf70typ() == 0) // General purpose CP
                {
                    ioInterrupts += cpudata.smf70slh() + cpudata.smf70tpi();
                    io += cpudata.smf70nio();
                }
            }
            return this;
        }
        
        // Add GCP dispatch data for all partitions.
        public IntervalData addPartitionData(Smf70Record r70)
        {
            // we use the CPU identification section to check if the CPUs are GCPs ("CP")
            List<CpuIdentificationSection> idSections = r70.cpuIdentificationSections();

            for (PrismPartitionDataSection partition : r70.prismPartitionDataSections())
            {
                // we need to report values for the system that wrote the record separately, as well
                // as totals for all partitions.
                // so we check whether the partition number matches the partition number in the 
                // RMF product section
                boolean homepartition = (partition.smf70lpn() == r70.productSection().smf70ptn());
                
                // skip to the Logical Processor Data sections for this partition, and process
                // the number of sections indicated by smf70bdn
                for (int i= (int)partition.smf70bds(); 
                        i < (int)partition.smf70bds() + partition.smf70bdn(); 
                        i++)
                {
                    // get the Logical Processor Data section
                    PrismLogicalProcessorDataSection lpd = r70.prismLogicalProcessorDataSections().get(i);
                    // Check the id section to see whether it is a GCP (smf70cix index values start at 1)
                    if (idSections.get(lpd.smf70cix()-1) 
                            .smf70cin().equals("CP"))        
                    {
                        allEdt += lpd.smf70edtSeconds(); // update total dispatch time
                     // if it is the partition that wrote the record, update home partition dispatch time
                        if (homepartition)  
                        {
                            myEdt += lpd.smf70edtSeconds();
                        }
                    }   
                }
            }
            return this;
        } 
    }
    
    /**
     * A class to accumulate data from multiple intervals and perform calculations.
     */
    private static class CumulativeData
    {
        private double cumulativeCps;
        private double totalseconds;
        private long ioInterrupts = 0;
        private long io = 0;
        private double myEdt = 0;
        private double allEdt = 0;
        
        CumulativeData (Iterable<IntervalData> data)
        {
            for (IntervalData entry : data)
            {
                // I suspect that we always have the same number of GCPs for a 
                // specific model, but I'm not sure so we'll calculate a weighted 
                // average CP count that allows for different numbers of CPs,
                // varying length intervals etc.
                
                cumulativeCps += (double)entry.cpCount * entry.intervalSeconds();
                totalseconds += entry.intervalSeconds();
                ioInterrupts += entry.ioInterrupts;
                io += entry.io;
                myEdt += entry.myEdt;
                allEdt += entry.allEdt;
            }
        }
        
        // various calculated values for the combined intervals
        
        /**
         * CP count, as an average value weighted by interval length
         * @return CP count
         */
        public double avgCps() {           
            return cumulativeCps / totalseconds;
        }

        /**
         * LPAR GCP busy calculated as a number of CPUs
         * @return LPAR GCP busy number of CPUs
         */
        public double lparBusyCps() {
            return myEdt / totalseconds;
        }

        /**
         * All partitions GCP busy calculated as a number of CPUs
         * @return All partitions GCP busy number of CPUs
         */
        public double allBusyCps() {
            return allEdt  / totalseconds;
        } 
               
        /**
         * LPAR GCP busy calculated as a percentage of all GCPs 
         * @return LPAR GCP busy percent
         */
        public double lparCpuPct() {
            return myEdt / (avgCps() * totalseconds) * 100;
        }
        
        /**
         * All partitions GCP busy calculated as a percentage of all GCPs
         * @return All partitions GCP busy percent
         */
        public double allCpuPct() {
            return allEdt / (avgCps() * totalseconds) * 100;
        }
        
        /**
         * LPAR I/O interrupts per second
         * @return I/O interrupts per second
         */
        public long getIoInterruptsPerSecond() {
            return (long)(ioInterrupts / totalseconds);
        }
        
        /**
         * LPAR I/O Interrupts per second per busy CP
         * @return I/O interrupts per second
         */
        public long getIoInterruptsPerSecondPerCp() {
            return (long)(ioInterrupts / totalseconds / lparBusyCps());
        }

        /**
         * LPAR I/Os per second
         * @return I/Os  per second
         */
        public long getIosPerSecond() {
            return (long)(io / totalseconds);
        }
        
        /**
         * LPAR I/Os per second per busy CP
         * @return I/Os per second
         */
        public long getIosPerSecondPerCp() {
            return (long)(io / totalseconds / lparBusyCps());
        }
    }     
}
