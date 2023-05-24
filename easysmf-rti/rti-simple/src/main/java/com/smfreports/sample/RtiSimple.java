package com.smfreports.sample;

import java.io.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.realtime.*;

/**
 * A simple demonstration of the EasySMF-RTI connection to the
 * SMF Real Time Interface.
 * <p>
 * This program connects to an in memory resource, and prints
 * the date/time, record type and size for each record it receives.
 * It disconnects from the in memory resource and exits after
 * 100 records, or when the MVS STOP command is received.
 * <p>
 * If data is missed (the in memory resource wraps before the
 * data is read) it prints a message and suppresses the 
 * exception.
 *
 */
public class RtiSimple
{
    public static void main(String[] args) throws IOException
    {              
        if (args.length < 1)
        {
            System.out.println("Usage: RtiSimple <resource-name>");
            return;
        }
        
        String inMemoryResource = args[0];
        
        // Connect to the resource, specifying a missed data handler
        // and to disconnect when a STOP command is received.
        // try-with-resources block automatically closes the 
        // SmfConnection and SmfRecordReader when leaving the block
        try (SmfConnection connection = 
                 SmfConnection.resourceName(inMemoryResource)
                     .onMissedData(RtiSimple::handleMissedData)
                     .disconnectOnStop()
                     .connect();
                
             // Use SmfRecordReader to create the records    
             SmfRecordReader reader = 
                 SmfRecordReader.fromByteArrays(connection))
        {
            int count = 0;
            // read and count records, and print information
            for (SmfRecord record : reader)
            {
                count++;
                System.out.format("%-24s record type: %4d size: %5d%n",
                        record.smfDateTime(),
                        record.recordType(),
                        record.length());
                if (count >= 100) break;
            }
            // SmfConnection and SmfRecordReader automatically closed here 
            // by try-with-resources block
        }
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
