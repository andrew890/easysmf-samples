package com.smfreports.sample;

import java.io.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.realtime.*;

public class EasySmfRtiSimple
{
    public static void main(String[] args) throws IOException
    {              
        if (args.length < 1)
        {
            System.out.println("Usage: EasySmfRtiSimple <resource-name>");
            return;
        }
        
        try (SmfConnection rti = 
                 SmfConnection.resourceName(args[0])
                     .onMissedData(EasySmfRtiSimple::handleMissedData)
                     .disconnectOnStop()
                     .connect();
                
             SmfRecordReader reader = 
                 SmfRecordReader.fromByteArrays(rti))
        {
            int count = 0;
            for (SmfRecord record : reader)
            {
                count++;
                System.out.format("%-24s record type: %4d size: %5d%n",
                        record.smfDateTime().toString(),
                        record.recordType(),
                        record.length());
                if (count >= 100) break;
            }
        }
    }
    
    static void handleMissedData(MissedDataEvent e)
    {
        System.out.println("Missed Data!");
        e.throwException(false);    
    }
}
