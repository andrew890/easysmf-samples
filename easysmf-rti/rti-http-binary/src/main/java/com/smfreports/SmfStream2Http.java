package com.smfreports;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;

import com.blackhillsoftware.smf.realtime.*;

public class SmfStream2Http
{
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException 
	{
        if (args.length < 2)
        {
            System.out.println("Usage: SmfStream2Http <resource-name> <url>");
            return;
        }
        
        String inMemoryResource = args[0];
        String url = args[1];
        
        // Create a HttpClient and request builder (requires Java 11) 
        HttpClient client = 
            HttpClient.newBuilder()
                .build();    
        Builder requestBuilder = 
            HttpRequest.newBuilder(new URI(url))
                .header("Content-Type", "application/octet-stream");
        
        // Create the connection, to be closed when a MVS STOP command is received.
		try (SmfConnection rti = 
				SmfConnection
					.resourceName(inMemoryResource) 
					.disconnectOnStop()
					.onMissedData(SmfStream2Http::handleMissedData)
					.connect())
		{
		    // We can send multiple SMF records in a single post, concatenated 
		    // in a byte array.
		    // We queue them in a ByteArrayOutputStream, and send them when 
		    // the SmfConnection tells us there are no records queued in the 
		    // connection i.e. the next read is likely to wait, or if the 
		    // total queued bytes reaches a threshold
		    
		    ByteArrayOutputStream outputQueue = new ByteArrayOutputStream();

			for (byte[] record : rti) // read next SMF record
			{
				outputQueue.write(record); // add it to our queue
				// if nothing more currently available, or 10MB queued
				if (rti.isEmpty()
						|| outputQueue.size() > 10 * 1024 * 1024)   
				{
					sendData(client, requestBuilder, outputQueue.toByteArray());             
                    outputQueue= new ByteArrayOutputStream(); // new empty queue 
				}
			}
			if (outputQueue.size() > 0) // send any remaining data
			{
                sendData(client, requestBuilder, outputQueue.toByteArray());             			    
			}
		} 	
	}

    private static void sendData(HttpClient client, Builder requestBuilder, byte[] payload)
            throws IOException, InterruptedException 
    {
        // http post records
        
        HttpRequest request = 
            requestBuilder
                .POST(BodyPublishers.ofByteArray(payload))
                .build();					
        HttpResponse<String> response =
            client.send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200)
        {
            System.out.println(response.statusCode() + " " + response.body());
        }
    }

	static void handleMissedData(MissedDataEvent e)
	{
		System.out.println("Missed Data!");
		e.throwException(false);	
	}
}
