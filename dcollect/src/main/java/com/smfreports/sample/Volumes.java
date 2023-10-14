package com.smfreports.sample;

import java.io.*;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;

public class Volumes
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: Volumes <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, StorageGroupInfo> storageGroups = new HashMap<>();
        
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.V))
                .map(Volume::from)
                .forEach(volumerecord -> {
                    storageGroups.computeIfAbsent(
                            volumerecord.dcvstggp(), 
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
                
                System.out.format("%-30s %,10d %,10.0f %,10.0f %10.1f %,10.0f%n",
                        entry.getKey().length() > 0 ? entry.getKey() : "Non-SMS",
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
                        entry.getKey().length() > 0 ? entry.getKey() : "Non-SMS");
                
                System.out.format("%-8s %-10s %10s %10s %10s %10s%n", 
                        "Volume",
                        "Mount",
                        "Tot MB",
                        "Free MB",
                        "Used%",
                        "Lrgst MB");
                
                sgi.volumes.stream()
                    .sorted(Comparator.comparing(vol -> vol.volser))
                    .forEachOrdered(vol -> {
                        
                        System.out.format("%-8s %-10s %,10.0f %,10.0f %10.1f %,10.0f%n", 
                                vol.volser,
                                vol.mount,
                                vol.spaceMB,
                                vol.freeMB,
                                (vol.spaceMB - vol.freeMB) / vol.spaceMB * 100,
                                vol.largestMB);
                        
                    });            
            });
    }
    
    private static class StorageGroupInfo
    {    
        int volCount = 0;
        double spaceMB = 0;
        double freeMB = 0;
        double largestMB = 0;
        
        List<VolumeInfo> volumes = new ArrayList<>();
        
        void add(Volume ds)
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
        double spaceMB;
        double freeMB;
        double largestMB;
        String mount;
        
        public VolumeInfo(Volume ds)
        {
            volser = ds.dcvvolsr();
            spaceMB = ds.dcvvlcapMB();
            freeMB = ds.dcvfrespMB();
            largestMB = ds.dcvlgextMB();
            if (ds.dcvuspvt())
            {
                mount = "PRIVATE";
            }
            else if (ds.dcvussto())
            {
                mount = "STORAGE";
            }
            else if (ds.dcvuspub())
            {
                mount = "PUBLIC";
            }
        }
    }
}
