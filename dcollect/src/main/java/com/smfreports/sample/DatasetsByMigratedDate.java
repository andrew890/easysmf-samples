package com.smfreports.sample;

import java.io.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.Map.Entry;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class DatasetsByMigratedDate
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: DatasetsByMigratedDate <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        TreeMap<LocalDate, Summary> datasetsByMigDate = new TreeMap<>();
        
        LocalDate rundate = LocalDate.now();
        
        datasetsByMigDate.put(rundate, new Summary());
        datasetsByMigDate.put(rundate.minusDays(7), new Summary());
        datasetsByMigDate.put(rundate.minusMonths(1), new Summary());
        datasetsByMigDate.put(rundate.minusMonths(6), new Summary());
        datasetsByMigDate.put(rundate.minusYears(1), new Summary());
        datasetsByMigDate.put(rundate.minusYears(5), new Summary());
                
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(r -> r.dcurctyp().equals(DcollectType.M))
                .map(MigratedDataset::from)
                .forEach(record -> 
                {
                    
                    Entry<LocalDate, Summary> agegroup = datasetsByMigDate.ceilingEntry(record.umdate());
                    if (agegroup == null)
                    {
                        // Not expected to happen, but maybe migrate date in the future?
                        // Use last (newest) entry
                        agegroup = datasetsByMigDate.lastEntry();
                    }
                    agegroup.getValue().add(record);
                });
        }
        
        System.out.format("%-8s %8s %10s %10s%n",
                "Migrated",
                "Count",
                "Migrat MB",
                "Size MB");
        
        datasetsByMigDate.entrySet().stream()
            .forEachOrdered(entry -> {
                System.out.format("%-8s %8d %10.1f %10.1f%n", 
                        Period.between(entry.getKey(), rundate),
                        entry.getValue().count,
                        entry.getValue().migratedMB,
                        entry.getValue().recallSpaceMB);
            });
    }
    
    private static class Summary
    {
        void add(MigratedDataset ds)
        {
            count++;
            migratedMB += ds.umdsizeMB();
            recallSpaceMB += ds.umrecspMB();
        }
        
        int count = 0;
        double migratedMB = 0;
        double recallSpaceMB = 0;
    }
    
}
