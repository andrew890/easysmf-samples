package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.summary.Top;

public class DeltaHlq
{
    private static boolean includeDataset(ActiveDataset datasetRecord)
    {
        //return true;
        
        if (
                datasetRecord.dcdstogp().equals("SG1")           
           )
        {
            return true;
        }
        return false;
    }
    
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("Usage: DeltaHlq <input-name-1> <input-name-2>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, HlqInfo> hlqs = new HashMap<>();
        
        // Stream first DCOLLECT file 
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .filter(DeltaHlq::includeDataset)
                .forEach(datasetrecord -> {
                    // Add A values
                    hlqs.computeIfAbsent(hlq(datasetrecord.dcddsnam()), 
                            key -> new HlqInfo())
                        .addA(datasetrecord);
                });
        }
        
        // Repeat for second DCOLLECT file 
        try (VRecordReader reader = VRecordReader.fromName(args[1]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .filter(DeltaHlq::includeDataset)                
                .forEach(datasetrecord -> {
                    // Add B values
                    hlqs.computeIfAbsent(hlq(datasetrecord.dcddsnam()), 
                            key -> new HlqInfo())
                        .addB(datasetrecord);
                });
        }
        
        reportDeltas(hlqs);              
    }

    private static String hlq(String dsn)
    {
        String[] qualifiers = dsn.split("\\.", 2);
        return qualifiers[0];
    }
    
    private static void reportDeltas(Map<String, HlqInfo> hlqs) 
    {        
        System.out.format("%-8s %10s %10s %10s %12s %12s %12s%n",
                "HLQ",
                "Count A",
                "Count B",
                "Change",
                "MB A",
                "MB B",
                "Change MB"
                );
        
        hlqs.entrySet().stream()
            .filter(entry -> entry.getValue().absChange() > 0)
            .collect(Top.values(20, 
                    Comparator.comparing(entry -> entry.getValue().absChange())))
            .forEach(entry -> {
                HlqInfo hlqinfo = entry.getValue();             

                System.out.format("%-8s %,10d %,10d %+,10d %,12d %,12d %+,12d%n",
                        entry.getKey(),
                        hlqinfo.countA,
                        hlqinfo.countB,
                        hlqinfo.deltaCount(),
                        hlqinfo.spaceMBA,
                        hlqinfo.spaceMBB,
                        hlqinfo.deltaSpace());
            });
    }
    
    private static class HlqInfo
    {    
        int countA = 0;
        long spaceMBA = 0;
        
        int countB = 0;
        long spaceMBB = 0;
        
        int deltaCount() { return countB - countA; }
        long deltaSpace() { return spaceMBB - spaceMBA; }
        
        public long absChange() { return Math.abs(deltaSpace()); }
        
        void addA(ActiveDataset ds)
        {
            countA++;
            spaceMBA += ds.dcdallsxMB();
        }
        
        void addB(ActiveDataset ds)
        {
            countB++;
            spaceMBB += ds.dcdallsxMB();
        }
    }

}
