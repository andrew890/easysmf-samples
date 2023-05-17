package com.blackhillsoftware;

import java.io.*;
import com.blackhillsoftware.smf.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * This class does nothing other than receive HTTP post requests and 
 * display the result. It is intended to demonstrate that the
 * data arrives intact at a HTTP server.
 */
public class EasySMFServlet extends HttpServlet
{    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {   	
    	System.out.println("Received post...");
    	
    	switch(request.getContentType())
    	{
    	case "application/octet-stream":
            receiveSmfData(request);   
            response.setStatus(HttpServletResponse.SC_OK);
            break;
    	case "application/json":
            receiveJson(request);
            response.setStatus(HttpServletResponse.SC_OK);   
            break;
            
    	default:
    	    System.out.println("Content type not implemented: " + request.getContentType());
            response.setStatus(HttpServletResponse.SC_OK);   
    	    break;
    	}
    }

    private void receiveSmfData(HttpServletRequest request) throws IOException 
    {
        InputStream inputStream = request.getInputStream();
        
        try (SmfRecordReader reader = SmfRecordReader.fromStream(inputStream))
        { 
            for (SmfRecord record : reader)
            {
                System.out.format("%24s SMF type: %5d Size: %5d%n", record.smfDateTime(), record.recordType(), record.recordLength());
            }
        }
    }

    private void receiveJson(HttpServletRequest request) throws IOException 
    {
        try (BufferedReader reader = request.getReader())
        {
            String line;
            while((line = reader.readLine()) != null)
            {
                System.out.println(line);
            }
        }
    }
}