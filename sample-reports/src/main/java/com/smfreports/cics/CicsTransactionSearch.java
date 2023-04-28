package com.smfreports.cics;

import java.io.*;
import java.time.LocalDateTime;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;

public class CicsTransactionSearch 
{
    public static void main(String[] args) throws IOException 
    {
        if (args.length < 1)
        {
            System.out.println("Usage: CicsTransactionSearch <input-name> <input-name2> ...");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        for (String name : args)
        {
            try (SmfRecordReader reader = SmfRecordReader.fromName(name)
                    .include(110, 1)) 
            {     
                reader.stream()
                
//                    .filter(record -> record.smfDateTime().isAfter(LocalDateTime.of(2023, 04, 26, 12, 50, 0)))
//                    .filter(record -> record.smfDateTime().isBefore(LocalDateTime.of(2023, 04, 26, 14, 10, 0)))
                
                    .map(r110 -> Smf110Record.from(r110))
                    .filter(r110 -> r110.haveDictionary())
//                    .filter(r110 -> r110.mnProductSection().smfmnprn().equals("CICSCA8A"))
                    
                    .map(r110 -> r110.performanceRecords())
                    
                    .flatMap(entries -> entries.stream())
 
//                    .filter(tx -> tx.getField(Field.TRAN).equals("IHEL"))
//                    .filter(tx -> tx.getField(Field.PGMNAME).equals("ICC$HEL"))
                    
//                    .filter(tx -> tx.getField(Field.DSPDELAY).timerSeconds() > 0.001)
//                    .filter(tx -> tx.elapsedSeconds() > 5)
                    
//                    .filter(tx -> tx.getField(Field.USRCPUT).timerSeconds() > 5)
                    
//                    .filter(tx -> !tx.getField(Field.CLIPADDR).equals("") 
//                           && !tx.getField(Field.CLIPADDR).startsWith(("172.")))
                    
                    .forEach(tx -> System.out.println(tx.toString()));
            }
        }
    }
}
