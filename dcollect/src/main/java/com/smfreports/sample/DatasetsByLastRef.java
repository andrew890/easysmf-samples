package com.smfreports.sample;

import java.io.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.Map.Entry;

import com.blackhillsoftware.dcollect.ActiveDataset;
import com.blackhillsoftware.dcollect.DcollectRecord;
import com.blackhillsoftware.dcollect.DcollectType;
import com.blackhillsoftware.smf.*;

public class DatasetsByLastRef
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: DatasetsByLastRef <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        TreeMap<LocalDate, DatasetGroup> datasetsByAge = new TreeMap<>();
        
        LocalDate rundate = LocalDate.now();
        
        datasetsByAge.put(rundate, new DatasetGroup());
        datasetsByAge.put(rundate.minusDays(7), new DatasetGroup());
        datasetsByAge.put(rundate.minusMonths(1), new DatasetGroup());
        datasetsByAge.put(rundate.minusMonths(6), new DatasetGroup());
        datasetsByAge.put(rundate.minusYears(1), new DatasetGroup());
        datasetsByAge.put(rundate.minusYears(5), new DatasetGroup());
                
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(r -> r.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .forEach(datasetRecord -> 
                {
                    // if last reference date is null, use creation date
                    LocalDate lastref = datasetRecord.dcdlstrf() != null ? datasetRecord.dcdlstrf() : datasetRecord.dcdcredt();
                    
                    Entry<LocalDate, DatasetGroup> agegroup = datasetsByAge.ceilingEntry(lastref);
                    if (agegroup == null)
                    {
                        // Not expected to happen, but maybe last reference date in the future?
                        // Use last (newest) entry
                        agegroup = datasetsByAge.lastEntry();
                    }
                    agegroup.getValue().add(datasetRecord);
                });
        }
        
        System.out.format("%-8s %8s %10s %10s%n",
                "Last Ref",
                "Count",
                "Alloc MB",
                "Used MB");
        
        datasetsByAge.entrySet().stream()
            .forEachOrdered(entry -> {
                System.out.format("%-8s %8d %10.1f %10.1f%n", 
                        Period.between(entry.getKey(), rundate),
                        entry.getValue().count,
                        entry.getValue().allocatedMB,
                        entry.getValue().usedMB);
            });
    }
    
    private static class DatasetGroup
    {
        void add(ActiveDataset ds)
        {
            count++;
            allocatedMB += ds.dcdallsxMB();
            usedMB += ds.dcdusesxMB();
        }
        
        int count = 0;
        double allocatedMB = 0;
        double usedMB = 0;
    }
    
}
