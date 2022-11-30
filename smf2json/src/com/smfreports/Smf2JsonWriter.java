package com.smfreports;

import java.io.*;

abstract class Smf2JsonWriter implements Closeable
{
    static Smf2JsonWriter forDD(String dd) throws IOException
    {
        return new Smf2JsonDDWriter(dd);        
    }
    static Smf2JsonWriter forFile(String filename) throws IOException
    {
        return new Smf2JsonFileWriter(filename);        
    }
    static Smf2JsonWriter forStdout() throws IOException
    {
        return new Smf2JsonStdoutWriter();        
    }
    abstract void writeLine(String output) throws IOException;
}
