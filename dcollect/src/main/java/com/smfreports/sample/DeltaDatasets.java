package com.smfreports.sample;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.summary.Top;

public class DeltaDatasets
{
    private static boolean includeDataset(ActiveDataset datasetRecord)
    {
        if (
               //datasetRecord.dcddsnam().startsWith("ANDREWR")
               //&&
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
            System.out.println("Usage: DeltaDatasets <input-name-1> <input-name-2>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        Map<String, DatasetInfo> datasets = new HashMap<>();
        
        // Stream first DCOLLECT file 
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(record -> record.dcurctyp().equals(DcollectType.D))
                .map(ActiveDataset::from)
                .filter(DeltaDatasets::includeDataset)
                .forEach(datasetrecord -> {
                    // Add A values
                    datasets.computeIfAbsent(maskIfGDG(datasetrecord.dcddsnam()), 
                            key -> new DatasetInfo())
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
                .filter(DeltaDatasets::includeDataset)                
                .forEach(datasetrecord -> {
                    // Add B values
                    datasets.computeIfAbsent(maskIfGDG(datasetrecord.dcddsnam()), 
                            key -> new DatasetInfo())
                        .addB(datasetrecord);
                });
        }
        
        reportDeltas(datasets);              
    }

    static Pattern gdgPattern  = Pattern.compile("(.+)\\.G\\d{4}V\\d{2}$");
    public static String maskIfGDG(String maybeGdg)
    {
        Matcher m = gdgPattern.matcher(maybeGdg);
        if (m.matches())
        {
            return m.group(1) + ".G####V##";
        }
        return maybeGdg;
    }
    
    private static void reportDeltas(Map<String, DatasetInfo> datasets) 
    {        
        System.out.format("%-44s %12s %12s %12s%n",
                "Dataset",
                "MB A",
                "MB B",
                "Change MB"
                );
        
        datasets.entrySet().stream()
            .filter(entry -> entry.getValue().absChange() > 0)
            .collect(Top.values(100, 
                    Comparator.comparing(entry -> entry.getValue().absChange())))
            .forEach(entry -> {
                DatasetInfo dataset = entry.getValue();             

                System.out.format("%-44s %,12d %,12d %+,12d%n",
                        entry.getKey(),
                        dataset.spaceMBA,
                        dataset.spaceMBB,
                        dataset.deltaSpace());
            });
    }
    
    private static class DatasetInfo
    {    
        long spaceMBA = 0;        
        long spaceMBB = 0;
        
        long deltaSpace() { return spaceMBB - spaceMBA; }   
        public long absChange() { return Math.abs(deltaSpace()); }
        
        void addA(ActiveDataset ds)
        {
            spaceMBA += ds.dcdallsxMB();
        }
        
        void addB(ActiveDataset ds)
        {
            spaceMBB += ds.dcdallsxMB();
        }
    }

}
