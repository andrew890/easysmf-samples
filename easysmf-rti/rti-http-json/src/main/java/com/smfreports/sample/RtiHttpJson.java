package com.smfreports.sample;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.realtime.*;
import com.blackhillsoftware.smf.smf30.*;
import com.google.gson.Gson;


/**
 * Read data from a SMF in memory resource, and post
 * information from job end records (type 30 subtype 5)
 * to a HTTP server in JSON format.  
 *
 */
public class RtiHttpJson 
{
    public static void main(String[] args) 
            throws IOException, InterruptedException, URISyntaxException 
    {
        if (args.length < 2)
        {
            System.out.println("Usage: RtiHttpJson <resource-name> <url>");
            return;
        }
        
        String inMemoryResource = args[0];
        String url = args[1];

        // Create a HttpClient and request builder (HttpClient requires Java 11) 
        HttpClient client = 
                HttpClient.newBuilder()
                    .build();
        Builder requestBuilder = 
                HttpRequest.newBuilder(new URI(url))
                    .header("Content-Type", "application/json");

        // Create a Gson instance to generate JSON, using EasySmfGsonBuilder
        // from EasySMF-JSON
        Gson gson = new EasySmfGsonBuilder()
                
                // make decimals more readable
                .avoidScientificNotation(true) 
                
                // exclude various data to reduce the size of the output 
                .includeZeroValues(false)  
                .includeUnsetFlags(false)
                .includeEmptyStrings(false)
                
                .createGson();
        
        // Create the connection, to be closed when a MVS STOP command is received
        // and wrap it in a SmfRecordReader to include only SMF 30 subtype 5
        try (SmfConnection connection = 
                SmfConnection.resourceName(inMemoryResource)
                    .disconnectOnStop()
                    .onMissedData(RtiHttpJson::handleMissedData)
                    .connect();
             SmfRecordReader reader =     
                SmfRecordReader.fromByteArrays(connection)
                    .include(30, 5)) 
        {
            // Read the records
            for (SmfRecord record : reader) 
            {
                // Create the SMF 30 record
                Smf30Record r30 = Smf30Record.from(record);
                
                // A job can generate multiple SMF 30 records, only
                // the first one has a Completion Section
                if (r30.completionSection() != null) 
                {
                    // Create an EasySMF-JSON CompositeEntry and add the interesting data
                    CompositeEntry compositeEntry = new CompositeEntry();
                    compositeEntry.add("time", r30.smfDateTime());
                    compositeEntry.add("id", r30.identificationSection());
                    compositeEntry.add("completion", r30.completionSection());
                    compositeEntry.add("proc", r30.processorAccountingSection());
                    compositeEntry.add("perf",r30.performanceSection());
                    compositeEntry.add("io",r30.ioActivitySection());
                    
                    // generate the JSON
                    String json = gson.toJson(compositeEntry);

                    // build the post request and send it
                    HttpRequest request = requestBuilder
                            .POST(BodyPublishers.ofString(json))
                            .build();
                    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

                    if (response.statusCode() != 200) 
                    {
                        System.out.println(response.statusCode() + " " + response.body());
                    }
                }             
            }
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
        e.throwException(false);
    }
}
