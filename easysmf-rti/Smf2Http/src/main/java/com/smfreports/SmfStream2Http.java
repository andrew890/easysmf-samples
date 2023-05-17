package com.smfreports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import com.blackhillsoftware.smf.realtime.*;

public class SmfStream2Http
{
	public static void main(String[] args) throws IOException, InterruptedException 
	{		    
		try (SmfConnection rti = 
				SmfConnection
					.resourceName(args[0])
					.disconnectOnStop()
					.onMissedData(x -> handleMissedData(x))
					.connect())
		{	
		    ByteArrayOutputStream queuedRecords = new ByteArrayOutputStream();
			
	    	try (CloseableHttpClient httpclient = HttpClients.createDefault()) 
	    	{
	    	    HttpPost httpPost = new HttpPost(args[1]);
			
				for (byte[] record : rti)
				{
					queuedRecords.write(record);
					if (!rti.hasNextImmediately() 
							|| queuedRecords.size() > 10 * 1024 * 1024) // nothing more currently available, or 10MB queued  
					{
						byte[] payload = queuedRecords.toByteArray();
						queuedRecords= new ByteArrayOutputStream();
						
						// http post records
		            	httpPost.setEntity(new ByteArrayEntity(payload, ContentType.APPLICATION_OCTET_STREAM));
		                try (CloseableHttpResponse response = httpclient.execute(httpPost)) 
		                {
		                	if (response.getCode() != 200)
		                	{
		                		System.out.println(response.getCode() + " " + response.getReasonPhrase());
		                	}
		                    HttpEntity entity2 = response.getEntity();
		                    EntityUtils.consume(entity2);
		                }
					}
				}
	    	}
		// Connection closed & resources freed automatically at the end of the try block 	
		} 	
	}	
	
	static void handleMissedData(MissedDataEvent e)
	{
		System.out.println("Missed Data!");
		e.throwException(false);	
	}
}
