package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.ActiveDataset;
import com.blackhillsoftware.dcollect.DcollectRecord;
import com.blackhillsoftware.dcollect.DcollectType;
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
                .filter(r -> r.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .forEach(record -> 
                {
                    String[] qualifiers = record.dcddsnam().split("\\.", 2);
                    
                    totalsByHlq.computeIfAbsent(qualifiers[0], key -> new Summary())
                        .add(record); 
                });
        }
        
        totalsByHlq.entrySet().stream()
            .sorted((a,b) -> Long.compare(b.getValue().allocatedMB, a.getValue().allocatedMB))
            .forEachOrdered(entry -> {
                System.out.format("%-8s %6d %6d %6d%n", 
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().allocatedMB,
                        entry.getValue().usedMB);
            });
        
        
    }
    
    private static class Summary
    {
        void add(ActiveDataset ds)
        {
            count++;
            allocatedMB += ds.dcdallsx() / 1024; // value is kb
            usedMB += ds.dcdusesx() / 1024;
        }
        
        int count = 0;
        long allocatedMB = 0;
        long usedMB = 0;
    }
    
}
