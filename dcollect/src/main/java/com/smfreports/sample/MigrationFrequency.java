package com.smfreports.sample;

import java.io.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.blackhillsoftware.dcollect.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.summary.Top;

public class MigrationFrequency
{
    private static boolean includeDataset(MigratedDataset dataset)
    {
        String datasetName = dataset.umdsnam();
        //if (datasetName.startsWith("ABCD")) return false;
        
        return true;
    }
    
    static LocalDate cutoff;
    static LocalDate runtime; 

    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: MigrationFrequency <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }

        runtime = LocalDate.now(); 
        cutoff = runtime.minusMonths(3); 

        System.out.format("%-44s %-15s %-15s %7s %7s %10s%n",
                "Dataset",
                "Created",
                "Migrated",
                "Count",
                "Freq",
                "Size MB");
                
        try (VRecordReader reader = VRecordReader.fromName(args[0]))
        {
            reader.stream()
                .map(DcollectRecord::from)
                .filter(r -> r.dcurctyp().equals(DcollectType.M))
                .map(MigratedDataset::from)
                
                .filter(r -> r.umnmig() > 2)
                .filter(r -> r.umdate().isAfter(cutoff))
                .filter(r -> r.umstgcl().length() > 0)
                .filter(r -> includeDataset(r))
                
                .collect(Top.values(100, Comparator.comparing(r -> frequency(r))))
                .forEach(record -> 
                {
                    System.out.format("%-44s %-15s %-15s %7d %7.1f %,10.1f%n", 
                            record.umdsnam(),
                            record.umcredt(),
                            record.ummdate().toLocalDate(),
                            record.umnmig(),
                            frequency(record), 
                            record.umrecspMB());
                });
        }
    }
        
    private static double frequency(MigratedDataset dataset)
    {
        // creation date only provided for SMS managed datasets
        if (dataset.umstgcl().length() == 0) return 0;
        
        long days = dataset.umcredt().until(runtime, ChronoUnit.DAYS);
        
        return (double)dataset.umnmig() / days * 365;
    }    
}
