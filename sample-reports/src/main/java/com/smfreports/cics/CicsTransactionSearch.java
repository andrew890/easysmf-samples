package com.smfreports.cics;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.*;
import com.blackhillsoftware.smf.cics.monitoring.DictionaryEntry;
import com.blackhillsoftware.smf.cics.monitoring.fields.*;
import com.google.gson.Gson;

public class CicsTransactionSearch 
{
    public static void main(String[] args) throws IOException 
    {
        if (args.length < 1)
        {
            System.out.println(
                "Usage: CicsTransactionSearch <input-name> <input-name2> ...");
            System.out.println(
                "<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
        
        // set up a Gson instance for optional JSON output 
        Gson gson = new EasySmfGsonBuilder() // set up a Gson instance for optional 
                //.setPrettyPrinting()
                .avoidScientificNotation(true) // make decimals more readable
                .cicsClockDetail(false)
                .includeZeroValues(false)
                .includeUnsetFlags(false)
                .includeEmptyStrings(false)
                .createGson();
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
        
        for (String name : args)
        {
            try (SmfRecordReader reader = SmfRecordReader.fromName(name)
                    .include(110, 1)) 
            {     
                reader.stream()
                                
                    // Filter by SMF record attributes (fastest)
                
                    // Filter by SMF record time 
//                    .filter(record -> record.smfDateTime()
//                         .isAfter(LocalDateTime.of(2023, 04, 26, 12, 50, 0)))
//                    .filter(record -> record.smfDateTime()
//                          .isBefore(LocalDateTime.of(2023, 04, 26, 14, 10, 0)))
                
                    // Filter by SMF ID
//                    .filter(record -> record.system().equals("S0W1"))
                
                    .map(r110 -> Smf110Record.from(r110))
                    .filter(r110 -> r110.haveDictionary())
                    
                    // Filter by APPLID 
//                    .filter(r110 -> r110.mnProductSection()
//                        .smfmnprn().equals("CICSABCD"))
                    
                    
                    // get the list of PerfromanceRecord/transactions
                    .map(r110 -> r110.performanceRecords())
                    // turn the multiple lists into a stream of transactions
                    .flatMap(entries -> entries.stream()) 
 
                    // Filter by transaction attributes
                    
                    // Filter by transaction time. Field is a ZonedDateTime (UTC)
                    // so we need to specify a ZoneId for the comparison. It doesn't
                    // have to be the same zone. It could be
                    // UTC "Z"
                    // An offset "-5"
                    // An IANA time zone name
//                    .filter(record -> record.getField(Field.STOP)
//                         .isAfter(ZonedDateTime.of(2023, 04, 26, 12, 50, 0, 0, 
//                                                   ZoneId.of("US/Pacific"))))
//                    .filter(record -> record.getField(Field.STOP)
//                          .isBefore(ZonedDateTime.of(2023, 04, 26, 14, 10, 0, 0, 
//                                                     ZoneId.of("US/Pacific"))))      
                    
                    // Filter by transaction
//                    .filter(tx -> tx.getField(Field.TRAN).equals("IHEL"))
                    
                    // Filter by program
//                    .filter(tx -> tx.getField(Field.PGMNAME).equals("ICC$HEL"))

                    // filter by elapsed time
//                    .filter(tx -> tx.elapsedSeconds() > 5)
                    
                    // filter by various field values, e.g. USRCPUT, DSPDELAY, IP Address
//                    .filter(tx -> tx.getField(Field.DSPDELAY).timerSeconds() > 0.1)
//                    .filter(tx -> tx.getField(Field.USRCPUT).timerSeconds() > 1)
//                    .filter(tx -> !tx.getField(Field.CLIPADDR).equals("") 
//                          && !tx.getField(Field.CLIPADDR).startsWith(("172.")))
                    
                    // Sort e.g. by transaction start time
                    // Note: sort means that all data matching the filter to this
                    // point will be retained in memory
//                    .sorted((a,b) -> a.getField(Field.START)
//                            .compareTo(b.getField(Field.START)))
                    
                    
                    // Limit number of matching entries
                    .limit(1000)
                    
                    .forEachOrdered(tx ->  // "Ordered" in case we applied a sort 
                    {
                        // Optional output types
                        
                        // Text
                        System.out.println(tx.toString());
 
                        // JSON
//                        System.out.println(gson.toJson(tx));
                        
                        // Custom format
//                        System.out.format("%-8s %-4s %-8s %-24s %-24s %s%n", 
//                                tx.smfmnprn(),
//                                tx.getField(Field.TRAN),
//                                tx.getField(Field.PGMNAME),
//                                tx.getField(Field.START),
//                                tx.getField(Field.STOP),
//                                tx.elapsed());
                    
                        // Write any CICS Clock fields where the time represents
                        // more than 5% of the elapsed time
//                        for (DictionaryEntry entry : tx.getDictionary().Entries())
//                        {
//                           if (entry.getFieldId() instanceof ClockField 
//                                    && tx.getClockField(entry).timerSeconds() > tx.elapsedSeconds() * 0.05)
//                            {
//                                System.out.println(entry.cmodhead() + " : " + tx.getClockField(entry).timer());
//                            }  
//                        }
                        
                    });
            }
        }
    }
}
