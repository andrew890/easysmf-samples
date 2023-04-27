package com.smfreports.cics;

import java.io.*;
import java.time.*;
import java.util.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.blackhillsoftware.smf.smf72.Smf72Record;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class CicsServiceClass 
{
    public static void main(String[] args) throws IOException 
    {
        if (args.length < 1)
        {
            System.out.println("Usage: CicsServiceClass <input-name> <input-name2> ...");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // Nested Maps to collect data
        // Applid -> Service Class -> Transaction name
        Map<String, Map<String, Map<String, TransactionData>>> txByApplidServiceClassTxName = new HashMap<>();
        // Applid -> Service Class
        Map<String, Map<String, TransactionData>> txByApplidServiceClass = new HashMap<>();

        Map<String, ServiceClassInfo> serviceClasses = new HashMap<>();
        
        int noDictionary = 0;
        int txCount = 0;

        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        for (String name : args)
        {
            try (SmfRecordReader reader = SmfRecordReader.fromName(name)
                    .include(110, 1)
                    .include(72,3)) 
            {     
                for (SmfRecord record : reader) 
                {
                    switch (record.recordType())
                    {
                    case 110:
                        
                        Smf110Record r110 = Smf110Record.from(record);
                        
                        if (r110.haveDictionary()) 
                        {
                            String applid = r110.mnProductSection().smfmnprn();
        
                            for (PerformanceRecord transaction : r110.performanceRecords()) 
                            {
                                txCount++;
                                // collect data by transaction name and also for all 
                                // transactions in a service class
                                txByApplidServiceClassTxName
                                        .computeIfAbsent(applid, key -> new HashMap<>())
                                        .computeIfAbsent(transaction.getField(Field.SRVCLSNM), key -> new HashMap<>())
                                        .computeIfAbsent(transaction.getField(Field.TRAN), key -> new TransactionData())
                                        .add(transaction);
                                
                                txByApplidServiceClass
                                    .computeIfAbsent(applid, key -> new HashMap<>())
                                    .computeIfAbsent(transaction.getField(Field.SRVCLSNM), key -> new TransactionData())
                                    .add(transaction);  
                            }
                        } 
                        else 
                        {
                            noDictionary++;
                        }
                        break;
                    case 72:
                        Smf72Record r72 = Smf72Record.from(record);
                        if (!r72.workloadManagerControlSection().r723mrcl()) // not a report class
                        {
                            String serviceClass = r72.workloadManagerControlSection().r723mcnm();
                            if(!serviceClasses.containsKey(serviceClass)
                                    || r72.smfDateTime().isAfter(serviceClasses.get(serviceClass).time))        
                            {
                                serviceClasses.put(serviceClass, new ServiceClassInfo(r72));
                            }
                        }
                    }
                }
            }
        }
        
        writeReport(serviceClasses, txByApplidServiceClass, txByApplidServiceClassTxName);
        
        System.out.format(
                "%n%nTotal Transactions: %,d%n", 
                txCount);
               
        if (noDictionary > 0) 
        {
            System.out.format(
                    "%n%nSkipped %,d records because no applicable dictionary was found.", 
                    noDictionary);
        }
        
        if (Smf110Record.getCompressedByteCount() > 0) 
        {
            System.out.format(
                    "%n%nCompressed bytes %,d, decompressed bytes %,d, compression %.1f%%.%n", 
                    Smf110Record.getCompressedByteCount(),
                    Smf110Record.getDecompressedByteCount(),
                    (double)(Smf110Record.getDecompressedByteCount() - Smf110Record.getCompressedByteCount()) 
                            / Smf110Record.getDecompressedByteCount() * 100);
        }
    }

    private static void writeReport(
            Map<String, ServiceClassInfo> serviceClasses,
            Map<String, Map<String, TransactionData>> txByApplidAll, 
            Map<String, Map<String, Map<String, TransactionData>>> txByApplidServiceClassTxName) 
    {        
        txByApplidServiceClassTxName.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(applidEntry ->
            {
                writeApplidServiceClasses(
                        serviceClasses,
                        applidEntry.getKey(),
                        txByApplidAll.get(applidEntry.getKey()),
                        txByApplidServiceClassTxName.get(applidEntry.getKey())
                        );
            });
    }

    private static void writeApplidServiceClasses(
            Map<String, ServiceClassInfo> serviceClasses,
            String applid,
            Map<String, TransactionData> txDataByApplidAll,
            Map<String, Map<String, TransactionData>> applidEntry) 
    {
        System.out.format("%nAPPLID: %-8s%n", applid);
        
        applidEntry.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(servClassEntry -> 
            {
                writeServiceClassTransactions(
                        serviceClasses,
                        servClassEntry.getKey(),
                        txDataByApplidAll.get(servClassEntry.getKey()),
                        applidEntry.get(servClassEntry.getKey())                 
                        );
            });
    }

    static final String headerfmt =  "%n        %-4s %15s %15s %15s %15s %15s %15s%n%n";
    static final String detailfmt =    "        %-4s %15d %15f %15f %15f %15f %15f%n";
    private static void writeServiceClassTransactions(
            Map<String, ServiceClassInfo> serviceClasses,
            String serviceClass,
            TransactionData all, 
            Map<String, TransactionData> servClassEntry) 
    {
        // Headings
        System.out.format("%n    Service Class: %-8s Description : %s Goal: %s%n", 
                serviceClass,
                serviceClasses.containsKey(serviceClass) ? serviceClasses.get(serviceClass).description : "",
                serviceClasses.containsKey(serviceClass) ? serviceClasses.get(serviceClass).goal : ""
                );
         
        System.out.format(headerfmt, 
                "Name", 
                "Count", 
                "Tot CPU", 
                "Avg CPU", 
                "Avg Elapsed",                        
                "Max Elapsed",                        
                "Std Dev");                        

        System.out.format(detailfmt, 
                "ALL",
                all.getCount(), 
                all.getCpu(), 
                all.getAvgCpu(), 
                all.getAvgElapsed(),
                all.getMaxElapsed(),
                all.getStandardDeviation());
        System.out.println();
        
        servClassEntry.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())                             
            .forEachOrdered(txDataEntry -> 
            {
                // write detail line
                System.out.format(detailfmt, 
                        txDataEntry.getKey(),
                        txDataEntry.getValue().getCount(), 
                        txDataEntry.getValue().getCpu(),
                        txDataEntry.getValue().getAvgCpu(),
                        txDataEntry.getValue().getAvgElapsed(),
                        txDataEntry.getValue().getMaxElapsed(),
                        txDataEntry.getValue().getStandardDeviation()); 
            });
    }
    
    private static class TransactionData 
    {
        public void add(PerformanceRecord txData) 
        {
            count++;
            double elapsed = Utils.ToSeconds(
                    Duration.between(txData.getField(Field.START), txData.getField(Field.STOP)));
            
            maxElapsed = maxElapsed > elapsed ? maxElapsed : elapsed;
            totalElapsed += elapsed;
            sd.increment(elapsed);
            
            cpu += txData.getField(Field.USRCPUT).timerSeconds();
        }

        public int getCount() 
        {
            return count;
        }

        public Double getMaxElapsed() 
        {
            return maxElapsed;
        }
        
        public Double getAvgElapsed() 
        {
            return count != 0 ? totalElapsed / count : null;
        }

        public Double getCpu() 
        {
            return cpu;
        }
        
        public Double getAvgCpu() 
        {
            return count != 0 ? cpu / count : null;
        }
        
        public Double getStandardDeviation() 
        {
            return sd.getResult();
        }

        private int count = 0;
        private double totalElapsed = 0;
        private double maxElapsed = 0;
        private double cpu = 0;
        // create a population standard deviation
        private StandardDeviation sd = new StandardDeviation(false); // population standard deviation
    }
    
    private static class ServiceClassInfo
    {
        ServiceClassInfo(Smf72Record r72)
        {
            this.description = r72.workloadManagerControlSection().r723mcde();
            this.goal = r72.serviceReportClassPeriodDataSections().get(0).goalDescription();
            this.time = r72.smfDateTime();
        }
        LocalDateTime time;
        String description;
        String goal;
    }
}
