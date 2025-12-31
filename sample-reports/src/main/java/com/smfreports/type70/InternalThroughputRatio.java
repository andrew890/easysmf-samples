package com.smfreports.type70;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;
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
     * Local date/time for before/after time processing. Set to null to 
     * use only the SMFID + machine combinations  
     */
    //private static LocalDateTime time = null;
    private static LocalDateTime time = LocalDateTime.of(2025, 11, 06, 12, 0);
    
    /**
     * Shifts for grouping the data. Add or remove shifts and 
     * update the selection logic as required.
     */
    private static enum Shift
    {
        ALL,
        SHIFT_1,
        SHIFT_2,
        SHIFT_3,
        WEEKEND_1,
        WEEKEND_2,
        WEEKEND_3;
        
        private static final LocalTime S1_END = LocalTime.of(8, 00);
        private static final LocalTime S2_END = LocalTime.of(16, 00);
        private static final List<DayOfWeek> WEEKEND = Arrays.asList(
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
            return ALL;
//            if (time.toLocalTime().isBefore(S1_END)) 
//                return WEEKEND.contains(time.getDayOfWeek()) ? WEEKEND_1 : SHIFT_1;
//            if (time.toLocalTime().isBefore(S2_END)) 
//                return WEEKEND.contains(time.getDayOfWeek()) ? WEEKEND_2 : SHIFT_2;            
//            return WEEKEND.contains(time.getDayOfWeek()) ? WEEKEND_3 : SHIFT_3;
        }
    }

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
                // Write Shift header
                System.out.format("%nShift: %s%n%n", shift.getKey());  

                // group data by system name+CPC and create cumulative data
                Map<Group, CumulativeData> cumulativeData = shift.getValue().stream()
                    .collect(Collectors.groupingBy(
                        entry -> entry.group,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            CumulativeData::new
                        )
                    ));

                reportShift(cumulativeData);
                
                // group data by CPC (machine), merging smfids with the same CPC
                Map<Group, CumulativeData> cumulativeDataByCpc = shift.getValue().stream()
                    .collect(Collectors.groupingBy(
                        entry -> new Group(
                            "CPC", // use literal "CPC" as smfid
                            entry.group.machine(),
                            entry.group.csc(),
                            entry.group.split()
                        ),
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            CumulativeData::new
                        )
                    ));

                reportShift(cumulativeDataByCpc);
            });
    }

    private static void reportShift(Map<Group, CumulativeData> cumulativeData) 
    {
        // Convert to list and sort by smfid, then by latestInterval
        List<Map.Entry<Group, CumulativeData>> sortedGroups = cumulativeData.entrySet().stream()
            .sorted(Comparator
                .comparing((Map.Entry<Group, CumulativeData> entry) -> entry.getKey().smfid)
                .thenComparing(entry -> entry.getValue().getLatestInterval(), 
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
        
        // Print calculated values table header
        System.out.format("%n%-30s %10s %12s %12s %10s %10s %12s %15s %10s %12s%n",
            "Group",
            "Avg CPs",
            "LPAR Busy",
            "All Busy",
            "LPAR CPU%",
            "All CPU%",
            "IO Int/sec",
            "IO Int/sec/CP",
            "IOs/sec",
            "IOs/sec/CP");
        System.out.println("-".repeat(140));
        
        // Report each group and its calculated values in table format
        for (Map.Entry<Group, CumulativeData> entry : sortedGroups)
        {
            Group group = entry.getKey();
            CumulativeData data = entry.getValue();
            
            System.out.format("%-30s %10.2f %12.2f %12.2f %10.2f %10.2f %12d %15d %10d %12d%n",
                group.toString(),
                data.avgCps(),
                data.lparBusyCps(),
                data.allBusyCps(),
                data.lparCpuPct(),
                data.allCpuPct(),
                data.getIoInterruptsPerSecond(),
                data.getIoInterruptsPerSecondPerCp(),
                data.getIosPerSecond(),
                data.getIosPerSecondPerCp());
        }
        
        // Calculate ratios for consecutive groups with the same smfid
        boolean headerPrinted = false;
        for (int i = 0; i < sortedGroups.size() - 1; i++)
        {
            Map.Entry<Group, CumulativeData> entry1 = sortedGroups.get(i);
            Map.Entry<Group, CumulativeData> entry2 = sortedGroups.get(i + 1);
            
            // Only calculate ratios if both groups have the same smfid
            if (entry1.getKey().smfid.equals(entry2.getKey().smfid))
            {
                // Print header on first ratio pair
                if (!headerPrinted)
                {
                    System.out.format("%nRatios:%n");
                    System.out.format("%-30s %-30s %12s %12s %10s %10s %12s %15s %10s %12s%n",
                        "Group 1",
                        "Group 2",
                        "LPAR Busy",
                        "All Busy",
                        "LPAR CPU%",
                        "All CPU%",
                        "IO Int/sec",
                        "IO Int/sec/CP",
                        "IOs/sec",
                        "IOs/sec/CP");
                    System.out.println("-".repeat(170));
                    headerPrinted = true;
                }
                
                Group group1 = entry1.getKey();
                Group group2 = entry2.getKey();
                CumulativeData data1 = entry1.getValue();
                CumulativeData data2 = entry2.getValue();
                
                System.out.format("%-30s %-30s %12.4f %12.4f %10.4f %10.4f %12.4f %15.4f %10.4f %12.4f%n",
                    group1.toString(),
                    group2.toString(),
                    data1.lparBusyCps() != 0 ? data2.lparBusyCps() / data1.lparBusyCps() : 0,
                    data1.allBusyCps() != 0 ? data2.allBusyCps() / data1.allBusyCps() : 0,
                    data1.lparCpuPct() != 0 ? data2.lparCpuPct() / data1.lparCpuPct() : 0,
                    data1.allCpuPct() != 0 ? data2.allCpuPct() / data1.allCpuPct() : 0,
                    data1.getIoInterruptsPerSecond() != 0 ? 
                        (double)data2.getIoInterruptsPerSecond() / data1.getIoInterruptsPerSecond() : 0,
                    data1.getIoInterruptsPerSecondPerCp() != 0 ? 
                        (double)data2.getIoInterruptsPerSecondPerCp() / data1.getIoInterruptsPerSecondPerCp() : 0,
                    data1.getIosPerSecond() != 0 ? 
                        (double)data2.getIosPerSecond() / data1.getIosPerSecond() : 0,
                    data1.getIosPerSecondPerCp() != 0 ? 
                        (double)data2.getIosPerSecondPerCp() / data1.getIosPerSecondPerCp() : 0);
            }
        }
    }
    
    /**
     * A class to collect the data from a system for an interval. 
     */
    private static class IntervalData
    {
        public IntervalData(Smf70Record r70)
        {
            group = new Group(r70.system(),
                    String.format("%04X-%s",
                            r70.cpuControlSection().smf70mod(), 
                            r70.cpuControlSection().smf70mdl()),
                    r70.cpuControlSection().smf70csc().replaceAll("^0+", ""), // strip leading zeros
                    (time == null) ? 
                            null 
                            : r70.smfDateTime().isBefore(time) ? 
                                    Split.BEFORE 
                                    : Split.AFTER
                            
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
                        .orElseThrow(() -> new RuntimeException("Could not find CP count, system: " + r70.system()));
        }
        
        private Group group;
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
        private LocalDateTime latestInterval = null;
        
        CumulativeData (List<IntervalData> data)
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
                if (latestInterval == null || entry.intervalEnd.isAfter(latestInterval))
                {
                    latestInterval = entry.intervalEnd;
                }
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
        
        /**
         * Get the latest interval end time
         * @return the latest interval end time
         */
        public LocalDateTime getLatestInterval() {
            return latestInterval;
        }
    }
    
    private static enum Split{BEFORE, AFTER};
    private static record Group(String smfid, String machine, String csc, Split split) 
    {
        @Override
        public String toString()
        {
            String result = String.format("%s %s %s", smfid, machine, csc);
            if (split != null) result = result + " " + split; 
            return result;
        }
    };
    
    
    // Useful constant for conversion to seconds
    private static final long NANOSPERSECOND = Duration.ofSeconds(1).toNanos(); 
}
