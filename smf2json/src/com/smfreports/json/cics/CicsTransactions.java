package com.smfreports.json.cics;

import java.io.IOException;
import java.util.*;

import com.blackhillsoftware.json.CompositeEntry;
import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.cics.Smf110Record;
import com.blackhillsoftware.smf.cics.monitoring.PerformanceRecord;
import com.blackhillsoftware.smf.cics.monitoring.fields.Field;
import com.blackhillsoftware.smf2json.cli.Smf2JsonCLI;

public class CicsTransactions {
    
    public static void main(String[] args) throws IOException
    {
        Smf2JsonCLI smf2JsonCLI = Smf2JsonCLI.create()
            .includeRecords(110, 1);
        smf2JsonCLI.easySmfGsonBuilder().avoidScientificNotation();
        smf2JsonCLI.start(new Client(), args);
    }
    
    private static class Client implements Smf2JsonCLI.Client
    {

        @Override
        public List<Object> processRecord(SmfRecord record) 
        {
            List<Object> result = new ArrayList<>();
            Smf110Record r110 = Smf110Record.from(record);
            for (PerformanceRecord perf : r110.performanceRecords())
            {
                result.add(new CompositeEntry()
                        .add("time", perf.getField(Field.STOP))
                        .add("system", r110.system())
                        .add("jobname", r110.mnProductSection().smfmnjbn())
                        .add("smfmnprn", r110.mnProductSection().smfmnprn())
                        .add("smfmnspn", r110.mnProductSection().smfmnspn())
                        .add("term", perf.getField(Field.TERM))
                        .add("tran", perf.getField(Field.TRAN))
                        .add("trannum", 
                                perf.isValidPacked(Field.TRANNUM) ?
                                        perf.getField(Field.TRANNUM) :
                                        perf.getFieldAsString(Field.TRANNUM).trim() 
                                )
                        .add("ttype", perf.getField(Field.TTYPE))
                        .add("start", perf.getField(Field.START))
                        .add("stop", perf.getField(Field.STOP))
                        .add("elapsed", perf.elapsedSeconds())
                        .add("dspdeley", perf.getField(Field.DSPDELAY).timerSeconds())
                        .add("usrcput", perf.getField(Field.USRCPUT).timerSeconds())
                        .add("usrdispt", perf.getField(Field.USRDISPT).timerSeconds())
                        .add("qrcput", perf.getField(Field.QRCPUT).timerSeconds())
                        .add("qrdispt", perf.getField(Field.QRDISPT).timerSeconds())
                        .add("susptime", perf.getField(Field.SUSPTIME).timerSeconds())
                        );   
            }
            return result;
        }
        
        @Override
        public List<Object> onEndOfData() 
        {
            System.err.println("Finished");
            return null;
        }


        
    }

}
