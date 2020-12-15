package com.smfreports.db2ddf;

import java.io.IOException;
import java.time.*;
import java.util.*;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.db2.Smf101Record;
import com.blackhillsoftware.smf.db2.section.*;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByKey;

public class ssidCorr
{   
    public static void main(String[] args) throws IOException
    {
        try (SmfRecordReader reader 
        		= SmfRecordReader.fromName(args[0])
    				.include(101))
        {   
        	Map<ReportKey, statistics> stats = new HashMap<>();
        	
            for (SmfRecord r : reader)
            {
                Smf101Record r101 = new Smf101Record(r);   
                Qwhc qwhc = r101.qwhc();
                
                if (r101.ifcid() == 3
                    && (qwhc.qwhcatyp() == QwhcConnectionType.QWHCDUW 
                        || qwhc.qwhcatyp() == QwhcConnectionType.QWHCRUW
                        || !r101.qlac().isEmpty())
                    && !r101.qmda().isEmpty())
                {
                	String ptyp = r101.qmda().get(0).qmdaptyp();
                	
            		if ((ptyp.equals("SQL") || ptyp.equals("DSN") || ptyp.equals("JCL"))
            		        && r101.qlac().get(0).qlacsqls() == 0)
            		{
		                stats.computeIfAbsent(new ReportKey(r101), key -> new statistics())
		                	.add(r101);		
                	}              	
                }
            }
            
            stats.entrySet().stream()          
                .sorted(comparingByKey(
                            comparing(ReportKey::getSmfid)
                                .thenComparing(ReportKey::getSsi)
                                .thenComparing(ReportKey::getCorrid)
                                .thenComparing(ReportKey::getDay)))
                    .forEachOrdered(entry ->
					{
						System.out.format("%s %s %s %s %6d %6d %6d %8.3f %8.3f %8.3f %8.3f %6d %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f%n", 
						        entry.getKey().getSmfid(), 
						        entry.getKey().getSsi(),
                                entry.getKey().getCorrid(),
                                entry.getKey().getDay(),
                                entry.getValue().count,
                                entry.getValue().commits,   
                                entry.getValue().aborts,   
                                entry.getValue().c1Tcb,
                                entry.getValue().c1Ziip,
                                entry.getValue().c2Tcb,
                                entry.getValue().c2Ziip,
                                entry.getValue().noZiip,
                                entry.getValue().ziipOnGcp,
                                entry.getValue().c2nnTcb,
                                entry.getValue().c2spTcb,
                                entry.getValue().c2udfTcb,
                                (double) (entry.getValue().c1Time.toMillis()) / 1000,
                                (double) (entry.getValue().c2Time().toMillis()) / 1000,
                                (double) (entry.getValue().c2nnTime.toMillis()) / 1000,
                                (double) (entry.getValue().c2spTime.toMillis()) / 1000,
                                (double) (entry.getValue().c2udfTime.toMillis()) / 1000,
                                (double) (entry.getValue().c2spSchTime.toMillis()) / 1000,
                                (double) (entry.getValue().c2udfSchTime.toMillis()) / 1000) ;   
					});
        }
    }
   
    static class statistics
    {
    	void add(Smf101Record r101)
    	{
    	    count++;
    	    
    	    Qwac qwac = r101.qwac().get(0);
    		commits += qwac.qwaccomm();
    		aborts += qwac.qwacabrt();
    			 		
    		c1Tcb += qwac.qwacejstSeconds()
    		        + qwac.qwacspcpSeconds()
    		        + qwac.qwacudcpSeconds();
    		
    		if (!qwac.qwacparr())
    		{
    		    c1Tcb -= qwac.qwacbjstSeconds();
    		}
    		
    		c1Ziip += qwac.qwaccls1ZiipSeconds();
    		
    		if (qwac.qwaccls1ZiipSeconds() == 0)
    		{
    		    noZiip++;	
    		}
    		
    		c2Tcb += qwac.qwacajstSeconds()
    		        + qwac.qwacspttSeconds()
    		        + qwac.qwacudttSeconds();

            c2Ziip += qwac.qwaccls2ZiipSeconds();
            
            ziipOnGcp += qwac.qwacziipEligibleSeconds();

            c2nnTcb += qwac.qwacajstSeconds();
            c2spTcb += qwac.qwacspttSeconds();  
            c2udfTcb += qwac.qwacudttSeconds();

            c1Time = c1Time.plus(
                    qwac.qwacparr() ?
                        Duration.between(stckOrigin, qwac.qwacesc()) :
                        Duration.between(qwac.qwacbsc(), qwac.qwacesc()));
         
            c2nnTime = c2nnTime.plus(qwac.qwacasc());
            c2spTime = c2spTime.plus(qwac.qwacspeb());
            c2udfTime = c2udfTime.plus(qwac.qwacudeb());
            c2spSchTime = c2spSchTime.plus(qwac.qwaccast());
            c2udfSchTime = c2udfSchTime.plus(qwac.qwacudst());

    	}
    	
    	
        long count = 0;
        long commits = 0;
        long aborts = 0;
        
        double c1Tcb = 0;
        double c1Ziip = 0;
        
        double c2Tcb = 0;
        double c2Ziip = 0;
        
        long noZiip = 0;

        double ziipOnGcp = 0;
        
        double c2nnTcb = 0;    
        double c2spTcb = 0;    
        double c2udfTcb = 0;
        
        Duration c1Time = Duration.ZERO;
        Duration c2nnTime = Duration.ZERO;
        Duration c2spTime = Duration.ZERO;
        Duration c2udfTime = Duration.ZERO;
        Duration c2spSchTime = Duration.ZERO;
        Duration c2udfSchTime = Duration.ZERO;
        
        Duration c2Time()
        {
            return c2nnTime
                    .plus(c2spTime)
                    .plus(c2udfTime)
                    .plus(c2spSchTime)
                    .plus(c2udfSchTime);
        }  
        
        private static final ZonedDateTime stckOrigin = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }       

    static class ReportKey
    {
        private String smfid;
        private String ssi;
        private String corrid;
        private LocalDate day;   
        
        public ReportKey(Smf101Record r101)
        {
            smfid = r101.system();
            ssi = r101.sm101ssi();
            corrid = getCorrid(r101);
            day = r101.smfDateTime().toLocalDate();
        }           
        
        public String getSmfid() {
            return smfid;
        }
        public String getSsi() {
            return ssi;
        }
        public String getCorrid() {
            return corrid;
        }
        public LocalDate getDay() {
            return day;
        }
        
        // We need hashCode and equals for use as a HashMap key. 
        // These were generated by Eclipse 
        
        @Override
        public int hashCode() {
            return Objects.hash(corrid, day, smfid, ssi);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ReportKey))
                return false;
            ReportKey other = (ReportKey) obj;
            return Objects.equals(corrid, other.corrid) 
                    && Objects.equals(day, other.day)
                    && Objects.equals(smfid, other.smfid) 
                    && Objects.equals(ssi, other.ssi);
        }
        
        private static String getCorrid(Smf101Record r101) 
        {
            String corrid = r101.qwhc().qwhccv();
            if (corrid.startsWith("ENTR") || corrid.startsWith("POOL"))
            {
                corrid = "CICS Tx " + corrid.substring(4, 8);
            }
            return corrid;
        }      
    }
}
