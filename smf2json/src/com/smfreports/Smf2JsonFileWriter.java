package com.smfreports;

import java.io.*;

class Smf2JsonFileWriter extends Smf2JsonWriter  
{
    Smf2JsonFileWriter(String filename) throws IOException
    {
        writer = new BufferedWriter(new FileWriter(filename));
    }
    
    private Writer writer;       
            
    @Override
    public void close() throws IOException {
        if (writer != null)
        {
            writer.close();
        }        
    }

    @Override
    public void writeLine(String output) throws IOException 
    {
        writer.write(output);
        writer.write(System.lineSeparator());
    }
}