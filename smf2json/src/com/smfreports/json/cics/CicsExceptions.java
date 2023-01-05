package com.smfreports.json.cics;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.json.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.Smf110Record;
import com.blackhillsoftware.smf.cics.monitoring.ExceptionData;
import com.blackhillsoftware.smf2json.cli.*;


public class CicsExceptions implements Smf2JsonCLI.Client 
{
    public static void main(String[] args) throws IOException                         
    {
        Smf2JsonCLI.create("Convert CICS Exception Records to JSON")
            .includeRecords(110)
            .start(new CicsExceptions(), args);
    }
    
    @Override
    public List<Object> processRecord(SmfRecord record) 
    {
        List<Object> result = new ArrayList<>();
        Smf110Record r110 = Smf110Record.from(record);
        for (ExceptionData exception : r110.exceptionData())
        {
            result.add(new CompositeEntry()                   
                    .add("time", exception.excmnsto())
                    .add("system", r110.smfsid())
                    .add("smfmnjbn", r110.mnProductSection().smfmnjbn())
                    .add("smfmnprn", r110.mnProductSection().smfmnprn())
                    .add("smfmnspn", r110.mnProductSection().smfmnspn())
                    .add(exception));
        }
        
        return result;
    }
}
