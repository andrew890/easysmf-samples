package com.smfreports;

import java.io.IOException;

import com.blackhillsoftware.zutil.io.TextRecordWriter;

class Smf2JsonDDWriter extends Smf2JsonWriter  
{
    Smf2JsonDDWriter(String ddname) throws IOException
    {
        writer = TextRecordWriter.newWriterForDD(ddname);
    }
    
    private TextRecordWriter writer;       
            
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
        writer.writeLine(output);
    }
}
