package com.smfreports.sample;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.realtime.*;
import com.blackhillsoftware.smf.smf70.*;
import com.blackhillsoftware.smf.smf70.subtype1.*;

public class SmtCheck
{    
    public static void main(String[] args) throws IOException
    {              
        if (args.length < 1 || (args[0].equals("--test") && args.length < 2))
        {
            System.out.println("Usage: RtiSimple [<resource-name>] | [--test <input-name>]");
            return;
        }
        
        boolean isTestMode = args[0].equals("--test");

        if (isTestMode)
        {
            try (SmfRecordReader reader = SmfRecordReader.fromName(args[1])
                        .include(70,1))
           {
               readRecords(reader, isTestMode);
           }
        }
        else
        {
            String inMemoryResource = args[0]; 
            // Connect to the resource, specifying a missed data handler
            // and to disconnect when a STOP command is received.
            // try-with-resources block automatically closes the 
            // SmfConnection when leaving the block
            try (SmfConnection connection = 
                     SmfConnection.forResourceName(inMemoryResource)
                         .onMissedData(SmtCheck::handleMissedData)
                         .disconnectOnStop()
                         .connect();
                 SmfRecordReader reader = SmfRecordReader.fromByteArrays(connection)
                         .include(70,1))
            {
                readRecords(reader, isTestMode);
            }
        }
    }

    private static void readRecords(SmfRecordReader reader, boolean isTestMode) 
    {
        BigInteger intervalToken = BigInteger.ZERO;
        List<Smf70Record> intervalRecords = new ArrayList<>();
        
        for (SmfRecord record : reader)
        {
            Smf70Record r70 = Smf70Record.from(record);
            
            if (!intervalToken.equals(r70.productSection().smf70ietRawValue()))
            {
                intervalToken = r70.productSection().smf70ietRawValue();
                intervalRecords.clear();
            }
            intervalRecords.add(r70);
            // we assume no duplicates, that should be safe using the SMF real time interface
            if (r70.productSection().smf70ran() == 0
                    || intervalRecords.size() >= r70.productSection().reassemblyArea().smf70rbr())
            {
                processInterval(intervalRecords, isTestMode);
                intervalRecords.clear();
            }                
        }
    }
    
    private static void processInterval(List<Smf70Record> intervalRecords, boolean isTestMode) {
        
        Optional<AsidDataAreaSection> asidData = intervalRecords.stream()
                .map(r70 -> r70.asidDataAreaSections())
                .flatMap(List::stream)
                .findFirst();
        
        boolean smt2 = isSmt2(intervalRecords);
        
        long processorCount = intervalRecords.stream()
                .filter(r70 -> r70.header().length() > 0x54)
                .map(r70 -> r70.cpuDataSections())
                .flatMap(List::stream)
                .filter(cpu -> cpu.smf70typ() == 2)        // zIIP
                .filter(cpu -> cpu.smf70sta())             // online at end of interval
                .filter(cpu -> cpu.smf70patSeconds() == 0) // parked time == 0
                .distinct()                                // CPU data sections can be repeated in broken records
                .collect(Collectors.counting());
        
        // missing data from the record - do nothing
        if (!asidData.isPresent() || processorCount == 0) return;
        
        if (smt2)
        {
            processorCount = processorCount / 2;
        }
        
        System.out.format("%n%-4s %-24s SMT: %-5s Processors %3d Min:%4d Max:%4d Avg:%4d ",
                intervalRecords.get(0).system(),
                intervalRecords.get(0).smfDateTime(),
                smt2,
                processorCount,
                asidData.get().smf70emn(),
                asidData.get().smf70emm(),
                asidData.get().smf70ett() / asidData.get().smf70srm()
                );
        

        if (smt2 && turnSmtOff(processorCount, asidData.get()))         
        {
            System.out.print("TURN SMT OFF");
        }
        else if (!smt2 && turnSmtOn(processorCount, asidData.get()))
        {
            System.out.print("TURN SMT ON");
        }
    }

    private static boolean isSmt2(List<Smf70Record> intervalRecords) 
    {
        boolean smt2 = intervalRecords.stream()
                .filter(r70 -> r70.header().length() > 0x54)
                .map(r70 -> r70.logicalCoreDataSections())
                .flatMap(List::stream)
                .filter(core -> core.smf70CpuNum() > 1)
                .findFirst()
                .isPresent() == true;
        return smt2;
    }
    
    // These numbers are plucked out of the air to provide an example and are 
    // not recommendations.
    // The criteria for turning SMT on and off needs to be researched and
    // tuned based on your own site requirements.
    private static boolean turnSmtOn(long processorCount, AsidDataAreaSection asidData)
    {
        long min = asidData.smf70emn();
        long max = asidData.smf70emm();
        long average = asidData.smf70ett() / asidData.smf70srm();
        
        if (min > processorCount) return true;
        if (max > processorCount * 3) return true;
        if (average > (float)processorCount * 1.5) return true; 
        
        return false;
    }

    private static boolean turnSmtOff(long processorCount, AsidDataAreaSection asidData)
    {
        long average = asidData.smf70ett() / asidData.smf70srm();

        if (turnSmtOn(processorCount, asidData)) return false;
        if (average < (float)processorCount * 1.2) return true;
        
        return false;
    }
    
    /**
     * Process the missed data event. This method prints a message
     * and indicates that an exception should not be thrown.
     * @param e the missed data event information
     */
    static void handleMissedData(MissedDataEvent e)
    {
        System.out.println("Missed Data!");
        // Suppress the exception
        e.throwException(false);    
    }
}
