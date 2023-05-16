package com.smfreports.sample;

import java.io.*;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.realtime.MissedDataEvent;
import com.blackhillsoftware.smf.realtime.SmfConnection;
import com.blackhillsoftware.smf.smf30.Smf30Record;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class EasySmfRtiNotifications
{
    public static void main(String[] args) throws IOException
    {       
        final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
        final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
        final String TO_PHONE = System.getenv("TO_PHONE");
        final String FROM_PHONE = System.getenv("FROM_PHONE");
          
        try (SmfConnection rti = 
                SmfConnection.resourceName(args[0])
                    .onMissedData(EasySmfRtiNotifications::handleMissedData)
                    .disconnectOnStop()
                    .connect())
        {
            try (SmfRecordReader reader = 
                    SmfRecordReader.fromByteArrays(rti)
                        .include(30))
            {
                for (SmfRecord record : reader)
                {
                    if (record.recordType() == 30 && record.subType() == 1)
                    {
                        Smf30Record r30 = Smf30Record.from(record);
                        if (r30.header().smf30wid().equals("TSO"))
                        {
                            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
                            Message message = Message.creator(
                                    new com.twilio.type.PhoneNumber(TO_PHONE),
                                    new com.twilio.type.PhoneNumber(FROM_PHONE),
                                    "TSO Login: " + r30.identificationSection().smf30jbn() + " " + r30.smfDateTime().toString())
                                .create();
                            System.out.println(message.getSid());
                        }
                    }
                }
            }
        }
    }
    
    static void handleMissedData(MissedDataEvent e)
    {
        System.out.println("Missed Data!");
        e.throwException(false);    
    }
}
