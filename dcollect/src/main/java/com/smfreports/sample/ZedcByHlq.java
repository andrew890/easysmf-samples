package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class ZedcByHlq
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: ZedcByHlq <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, ZedcSummary> zedcByHlq = new HashMap<>();
        
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(r -> r.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .filter(r ->r.dcdcmptv() && r.dcdczedc()) // comp type is valid and comp type is zedc
                .forEach(record -> 
                {
                    String[] qualifiers = record.dcddsnam().split("\\.", 2);
                    zedcByHlq.computeIfAbsent(qualifiers[0], key -> new ZedcSummary())
                        .add(record);                  
                });
        }
        
        System.out.format("%-8s %8s %12s %12s %7s%n", 
                "HLQ",
                "Count",
                "Comp MB",
                "Uncomp MB",
                "Comp%");
        
        zedcByHlq.entrySet().stream()
            .sorted((a,b) -> Double.compare(b.getValue().compressed, a.getValue().compressed))
            .forEachOrdered(entry -> {
                System.out.format("%-8s %,8d %,12.0f %,12.0f %7.1f%n", 
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().compressed,
                        entry.getValue().uncompressed,
                        entry.getValue().ratio());
            });
    }
    
    private static class ZedcSummary
    {
        void add(ActiveDataset ds)
        {
            count++;
            compressed += ds.dcdcudszMB();
            uncompressed += ds.dcdudsizMB();
        }
        
        int count = 0;
        double compressed = 0;
        double uncompressed = 0;
        
        double ratio() {return (1 - (compressed / uncompressed)) * 100;}   
    }
}
