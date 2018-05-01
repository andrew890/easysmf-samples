package com.smfreports;

import java.io.*;

import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.smf30.Smf30Record;

public class UserkeyCommonReport 
{
    public static void main(String[] args) throws IOException
    {
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromStream(new FileInputStream(args[0])))                
        {      
            reader.include(30,5)
            	.stream()
            	.map(record -> new Smf30Record(record))
            	.filter(r30 -> 
            		r30.storageSection() != null
            		&& r30.storageSection().smf30UserKeyCommonAuditEnabled()
            		&& (r30.storageSection().smf30UserKeyCsaUsage()
	            		|| r30.storageSection().smf30UserKeyCadsUsage()
	            		|| r30.storageSection().smf30UserKeyChangKeyUsage()))
                .forEach(r30 -> 
                {
                	System.out.format("%10s %10s %10s %10s %10s",  			
                		r30.identificationSection().smf30jbn(),
        				r30.identificationSection().smf30jnm(),
        				!r30.storageSection().smf30UserKeyCsaUsage() ? "CSA" : "",
                		!r30.storageSection().smf30UserKeyCadsUsage() ? "CADS" : "",
                        !r30.storageSection().smf30UserKeyChangKeyUsage() ? "KEYCHANGE" : "");
                });
        }
    }
}
