package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class DeltaStorageGroups
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("Usage: DeltaStorageGroups <input-name-1> <input-name-2>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, StorageGroupInfo> storageGroups = new HashMap<>();
        
        // Stream first DCOLLECT file 
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.VL))
                .map(SmsVolume::from)
                .forEach(volumerecord -> {
                    // Add A values
                    storageGroups.computeIfAbsent(volumerecord.dvlstgrp(), 
                            key -> new StorageGroupInfo())
                        
                        .addA(volumerecord);
                });
        }
        
        // Repeat for second DCOLLECT file 
        try (VRecordReader reader = VRecordReader.fromName(args[1]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.VL))
                .map(SmsVolume::from)
                .forEach(volumerecord -> {
                    // Add B values
                    storageGroups.computeIfAbsent(volumerecord.dvlstgrp(),
                            key -> new StorageGroupInfo())
                        .addB(volumerecord);
                });
        }
        
        reportDeltas(storageGroups);  
               
    }

    private static void reportDeltas(Map<String, StorageGroupInfo> storageGroups) 
    {        
        storageGroups.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> {
                StorageGroupInfo storagegroup = entry.getValue();             
                
                System.out.format("%nStorage Group : %-30s%n",
                        entry.getKey());
                
                System.out.format("%8s %10s %10s %10s %10s%n",
                        "",
                        "Volumes",
                        "Tot MB",
                        "Free MB",
                        "Used%");
                
                System.out.format("%8s %,10d %,10d %,10d %10.1f%n",
                        "A:",
                        storagegroup.volCountA,
                        storagegroup.spaceMBA,
                        storagegroup.freeMBA,
                        storagegroup.usedA());
                
                System.out.format("%8s %,10d %,10d %,10d %10.1f%n",
                        "B:",
                        storagegroup.volCountB,
                        storagegroup.spaceMBB,
                        storagegroup.freeMBB,
                        storagegroup.usedB());
                
                System.out.format("%8s %+,10d %+,10d %+,10d %+10.1f%n",
                        "Change:",
                        storagegroup.deltaVolumes(),
                        storagegroup.deltaSpace(),
                        storagegroup.deltaFree(),
                        storagegroup.deltaUsedPct());
            });
    }
    
    private static class StorageGroupInfo
    {    
        int volCountA = 0;
        long spaceMBA = 0;
        long freeMBA = 0;
        
        int volCountB = 0;
        long spaceMBB = 0;
        long freeMBB = 0;
        
        double usedA() {return (double)(spaceMBA - freeMBA) / spaceMBA * 100; }
        double usedB() {return (double)(spaceMBB - freeMBB) / spaceMBB * 100; }
        
        int    deltaVolumes() { return volCountB - volCountA; }
        long   deltaSpace()   { return spaceMBB - spaceMBA; }
        long   deltaFree()    { return freeMBB - freeMBA; }
        double deltaUsedPct() { return usedB() - usedA(); }
        
        void addA(SmsVolume ds)
        {
            volCountA++;
            spaceMBA += ds.dvlntcpy();
            freeMBA += ds.dvlnfree();
        }
        
        void addB(SmsVolume ds)
        {
            volCountB++;
            spaceMBB += ds.dvlntcpy();
            freeMBB += ds.dvlnfree();
        }
    }

}
