package com.smfreports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.blackhillsoftware.smf.realtime.*;

public class SmfStream2Http
{
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException 
	{		    
		try (SmfConnection rti = 
				SmfConnection
					.resourceName(args[0])
					.disconnectOnStop()
					.onMissedData(x -> handleMissedData(x))
					.connect())
		{	
		    ByteArrayOutputStream queuedRecords = new ByteArrayOutputStream();
		    
	        HttpClient client = HttpClient.newBuilder().build();    
		    Builder requestBuilder = HttpRequest
		            .newBuilder(new URI(args[1]))
		            .header("Content-Type", "application/octet-stream");
					
			for (byte[] record : rti)
			{
				queuedRecords.write(record);
				if (!rti.hasNextImmediately() 
						|| queuedRecords.size() > 10 * 1024 * 1024) // nothing more currently available, or 10MB queued  
				{
					byte[] payload = queuedRecords.toByteArray();
					queuedRecords= new ByteArrayOutputStream();
					
					// http post records
					
					HttpRequest request = requestBuilder.POST(BodyPublishers.ofByteArray(payload))
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

    private static void send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response =
            client.send(request, BodyHandlers.ofString());

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
