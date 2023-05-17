package com.smfreports;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.blackhillsoftware.json.EasySmfGsonBuilder;
import com.blackhillsoftware.json.util.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.realtime.*;
import com.blackhillsoftware.smf.smf30.Smf30Record;
import com.google.gson.Gson;

public class SmfJson2Http 
{
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException 
    {
        Gson gson = new EasySmfGsonBuilder().avoidScientificNotation(true) // make decimals more readable
                .includeZeroValues(false).includeUnsetFlags(false).includeEmptyStrings(false).createGson();

        HttpClient client = HttpClient.newBuilder().build();
        Builder requestBuilder = HttpRequest.newBuilder(new URI(args[1])).header("Content-Type", "application/json");

        try (SmfConnection rti = SmfConnection.resourceName(args[0])
                .disconnectOnStop()
                .onMissedData(x -> handleMissedData(x)).connect()) 
        {
            try (SmfRecordReader reader = SmfRecordReader.fromByteArrays(rti)
                    .include(30, 5)) 
            {
                for (SmfRecord record : reader) 
                {
                    Smf30Record r30 = Smf30Record.from(record);
                    if (r30.completionSection() != null) 
                    {
                        CompositeEntry ce = new CompositeEntry();
                        ce.add("time", r30.smfDateTime());
                        ce.add("id", r30.identificationSection());
                        ce.add("comppletion", r30.completionSection());
                        ce.add("proc", r30.processorAccountingSection());
                        ce.add("perf",r30.performanceSection());
                        ce.add("io",r30.ioActivitySection());
                        
                        String json = gson.toJson(ce);

                        // http post records

                        HttpRequest request = requestBuilder
                                .POST(BodyPublishers.ofString(json))
                                .build();

                        try 
                        {
                            send(client, request);
                        } 
                        catch (IOException e) 
                        {
                            System.out.println(e.toString());
                            System.out.println("will retry in 5 seconds");
                            Thread.sleep(5000);
                            // send the same request again
                            // if this fails too we allow the exception to propagate
                            send(client, request);
                        }
                        
                    }
                }
            }
        }
    }

    private static void send(HttpClient client, HttpRequest request) throws IOException, InterruptedException 
    {
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200) 
        {
            System.out.println(response.statusCode() + " " + response.body());
        }
        if (response.statusCode() >= 400) // some sort of error
        {
            throw new RuntimeException("Unrecoverable error");
        }
    }

    static void handleMissedData(MissedDataEvent e) 
    {
        System.out.println("Missed Data!");
        e.throwException(false);
    }
}
