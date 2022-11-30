package com.smfreports;

import java.io.IOException;

import com.blackhillsoftware.smf.SmfRecordReader;

class Smf2JsonInput 
{
    private Smf2JsonInput(String name, boolean isDD)
    {
        this.name = name;
        this.isDD = isDD;
    }
    
    static Smf2JsonInput dd(String dd)
    {
        return new Smf2JsonInput(dd, true);
    }

    static Smf2JsonInput file(String fileName)
    {
        return new Smf2JsonInput(fileName, false);
    }
    
    SmfRecordReader getReader() throws IOException
    {
        return isDD ? 
                SmfRecordReader.fromDD(name) :
                SmfRecordReader.fromName(name);
    }
    
    String name;
    boolean isDD; 
}
