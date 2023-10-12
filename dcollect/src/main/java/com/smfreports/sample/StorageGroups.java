package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class StorageGroups
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: StorageGroups <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, StorageGroupInfo> storageGroups = new HashMap<>();
        
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.VL))
                .map(SmsVolume::from)
                .forEach(volumerecord -> {
                    storageGroups.computeIfAbsent(volumerecord.dvlstgrp(), 
                            key -> new StorageGroupInfo())
                        .add(volumerecord);
                });
        }
        
        storageGroupSummary(storageGroups);  
        
        storageGroupVolumes(storageGroups);
        
    }

    private static void storageGroupSummary(Map<String, StorageGroupInfo> storageGroups) 
    {
        System.out.format("%-30s %10s %10s %10s %10s %10s%n",
                "Storage Group",
                "Volumes",
                "Tot MB",
                "Free MB",
                "Used%",
                "Lrgst MB");
        
        storageGroups.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> {
                StorageGroupInfo sgi = entry.getValue();             
                
                System.out.format("%-30s %,10d %,10d %,10d %10.1f %,10d%n",
                        entry.getKey(),
                        sgi.volCount,
                        sgi.spaceMB,
                        sgi.freeMB,
                        (double)(sgi.spaceMB - sgi.freeMB) / sgi.spaceMB * 100,
                        sgi.largestMB);
                
            });
    }
    
    private static void storageGroupVolumes(Map<String, StorageGroupInfo> storageGroups) {
        storageGroups.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> {
                StorageGroupInfo sgi = entry.getValue();
                System.out.format("%nStorage Group: %s%n%n", 
                        entry.getKey());
                
                System.out.format("%-8s %10s %10s %10s %10s%n", 
                        "Volume",
                        "Tot MB",
                        "Free MB",
                        "Used%",
                        "Lrgst MB");
                
                sgi.volumes.stream()
                    .sorted(Comparator.comparing(vol -> vol.volser))
                    .forEachOrdered(vol -> {
                        
                        System.out.format("%-8s %,10d %,10d %10.1f %,10d%n", 
                                vol.volser,
                                vol.spaceMB,
                                vol.freeMB,
                                (double)(vol.spaceMB - vol.freeMB) / vol.spaceMB * 100,
                                vol.largestMB);
                        
                    });            
            });
    }
    
    private static class StorageGroupInfo
    {    
        int volCount = 0;
        long spaceMB = 0;
        long freeMB = 0;
        long largestMB = 0;
        
        List<VolumeInfo> volumes = new ArrayList<>();
        
        void add(SmsVolume ds)
        {
            volCount++;
            VolumeInfo volumeInfo = new VolumeInfo(ds);
            spaceMB += volumeInfo.spaceMB;
            freeMB += volumeInfo.freeMB;
            largestMB = Math.max(largestMB, volumeInfo.largestMB);
            volumes.add(volumeInfo);
        }
    }
    
    private static class VolumeInfo
    {
        String volser;
        long spaceMB;
        long freeMB;
        long largestMB;
        
        public VolumeInfo(SmsVolume ds)
        {
            volser = ds.dvlvser();
            spaceMB = ds.dvlntcpy();
            freeMB = ds.dvlnfree();
            largestMB = ds.dvlnlext();
        }
    }
}
