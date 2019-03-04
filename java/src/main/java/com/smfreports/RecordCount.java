package com.smfreports;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;

/**
 *
 * Collect statistics for SMF records by type and subtype,
 * and print record count, megabytes and various statistics 
 * for each type/subtype 
 *
 */
public class RecordCount
{
    public static void main(String[] args) throws IOException
    {
        // Use a HashMap to keep the statistics
        // The key is an Integer (4 bytes), the record type is shifted
        // to the first 2 bytes, the subtype is in the second 2 bytes.
        Map<Integer, RecordStats> statsMap = new HashMap<Integer, RecordStats>();
        
        // If we received no arguments, open DD INPUT
        // otherwise use the first argument as the file 
        // name to read.        
        try (SmfRecordReader reader = args.length == 0 ?
            SmfRecordReader.fromDD("INPUT") :
            SmfRecordReader.fromName(args[0]))
        {
            for (SmfRecord record : reader)
            {
                int type = record.recordType();
                int subtype = record.hasSubtypes() ? record.subType() : 0;
                Integer key = (type << 16) + subtype;
                if (statsMap.containsKey(key)) 
                {
                    // Add to existing
                    statsMap.get(key).add(record);
                }
                else
                {
                    // Insert new
                    statsMap.put(key, new RecordStats(record));
                } 
            }
        }

        writeReport(statsMap);
    }

    private static void writeReport(Map<Integer, RecordStats> statsMap)
    {
        // get the total bytes from all record types
        long totalbytes = statsMap.entrySet().stream()
            .mapToLong(entry -> entry.getValue().getBytes())
            .sum();

        // write heading
        System.out.format("%5s %8s %11s %11s %7s %9s %9s %9s %n", 
            "Type", "Subtype", "Records", "MB", "Pct", "Min", "Max", "Avg");

        // write data
        statsMap.entrySet().stream()
            .map(entry -> entry.getValue())

            // sort by total bytes descending
            .sorted(Comparator.comparingLong(RecordStats::getBytes).reversed())

            // alternate sort, by type and subtype
            // .sorted(Comparator
            // .comparingInt(RecordStats::getRecordtype)
            // .thenComparingInt(RecordStats::getSubtype))

            .forEachOrdered(entry -> 
                System.out.format("%5d %8d %11d %11d %7.1f %9d %9d %9d %n", 
                    entry.getRecordtype(),
                    entry.getSubtype(), 
                    entry.getCount(), 
                    entry.getBytes() / (1024 * 1024),              
                    (float) (entry.getBytes()) / totalbytes * 100, 
                    entry.getMinLength(), 
                    entry.getMaxLength(),
                    entry.getBytes() / entry.getCount()));
    }

    /**
     * Statistics for a type/subtype combination
     */
    private static class RecordStats
    {
        /**
         * Initialize statistics for a new record type/subtype combination
         * @param record The first record of this group 
         */
        RecordStats(SmfRecord record)
        {
            this.recordtype = record.recordType();
            this.subtype = record.hasSubtypes() ? record.subType() : 0;;
            this.count = 1;
            int length = record.recordLength();
            bytes = length;
            minLength = length;
            maxLength = length;
        }

        /**
         * Add a record to the statistics
         * 
         * @param record a SMF record
         */
        public void add(SmfRecord record)
        {
            count++;
            int length = record.recordLength();
            bytes += length;
            minLength = (minLength == 0 || length < minLength) ? length : minLength;
            maxLength = length > maxLength ? length : maxLength;
        }

        private int  recordtype;
        private int  subtype;
        private int  count;
        private long bytes;
        private int  maxLength;
        private int  minLength;

        int getRecordtype() { return recordtype; }
        int getSubtype() { return subtype; }
        int getCount() { return count; }
        long getBytes() { return bytes; }
        int getMaxLength() { return maxLength; }
        int getMinLength() { return minLength; }    
    }
}
