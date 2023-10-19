package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class MbByHlq
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: MbByHlq <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, Summary> totalsByHlq = new HashMap<>();
        
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(r -> r.dcurctyp().equals(DcollectType.D) || r.dcurctyp().equals(DcollectType.M))
                .forEach(record -> 
                {
                    switch (record.dcurctyp())
                    {
                    case D:
                    {
                        ActiveDataset dataset = ActiveDataset.from(record);
                        String[] qualifiers = dataset.dcddsnam().split("\\.", 2);
                        totalsByHlq.computeIfAbsent(qualifiers[0], key -> new Summary())
                            .add(dataset);                  
                        break;
                    }
                    case M:
                        MigratedDataset dataset = MigratedDataset.from(record);
                        String[] qualifiers = dataset.umdsnam().split("\\.", 2);
                        totalsByHlq.computeIfAbsent(qualifiers[0], key -> new Summary())
                            .add(dataset);                          
                        break;
                    default:
                        break;
                    
                    }
                });
        }
        
        System.out.format("%-8s %8s %10s %8s %10s %10s %8s %10s %8s %10s%n", 
                "HLQ",
                "Count",
                "Total MB",
                "Level0",
                "Alloc MB",
                "Used MB",
                "ML1",
                "ML1 MB",
                "ML2",
                "ML2 MB");
        
        totalsByHlq.entrySet().stream()
            .sorted((a,b) -> Double.compare(b.getValue().level0Alloc, a.getValue().level0Alloc))
            .forEachOrdered(entry -> {
                System.out.format("%-8s %,8d %10.0f %,8d %10.0f %10.0f %,8d %10.0f %,8d %10.0f%n", 
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().totalMB,
                        entry.getValue().level0Count,
                        entry.getValue().level0Alloc,
                        entry.getValue().level0UsedMB,
                        entry.getValue().level1Count,
                        entry.getValue().level1MB,
                        entry.getValue().level2Count,
                        entry.getValue().level2MB);
            });
    }
    
    private static class Summary
    {
        void add(ActiveDataset ds)
        {
            count++;
            level0Count++;
            totalMB += ds.dcdallsxMB();
            level0Alloc += ds.dcdallsxMB();
            level0UsedMB += ds.dcdusesxMB();
        }
        
        void add(MigratedDataset ds)
        {
            count++;
            totalMB += ds.umrecspMB();
            // There are various space values in the record, we
            // will report the estimated space required if this
            // dataset was recalled to level 0
            
            switch (ds.umlevel())
            {
            case LEVEL1:
                level1Count++;
                level1MB += ds.umrecspMB();
                break;
            case LEVEL2:
                level2Count++;
                level2MB += ds.umrecspMB();
                break;
            default:
                throw new RuntimeException("Unexpected migration level: " + ds.umlevel());
            }
        }
        
        int count = 0;
        int level0Count = 0;
        
        double totalMB = 0;
        
        double level0Alloc = 0;
        double level0UsedMB = 0;
        
        int level1Count = 0;
        double level1MB = 0;
        
        int level2Count = 0;
        double level2MB = 0;
    }
    
}
