package com.smfreports;

import java.io.*;

import com.blackhillsoftware.smf.*;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;

public class Smf2Http
{
    public static void main(String[] args) throws IOException
    {
    	try (CloseableHttpClient httpclient = HttpClients.createDefault()) 
    	{
    	    HttpPost httpPost = new HttpPost("http://127.0.0.1:8080/easysmf");

	        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))		
	    	{
	            for (SmfRecord record : reader)
	            {
	            	httpPost.setEntity(new ByteArrayEntity(record.getBytes(), ContentType.APPLICATION_OCTET_STREAM));
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
    }
}
