package com.blackhillsoftware.samples;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.smf30.*;
import com.blackhillsoftware.smf.smf70.*;

public class Peak4HRAJobs {
    public static void main(String[] args) throws IOException                                   
    {                                                                                           
    	Map< String, 
    		Map< LocalDateTime, 
    			HourlyLac > > systemHourLAC 
    				= new HashMap<String, Map<LocalDateTime, HourlyLac>>();
    	Map< String, 
    		Map< LocalDateTime, 
    			Map< String, 
    				HourlyJobTotals > > > systemHourJobnameTotals 
    					= new HashMap<String, Map<LocalDateTime, Map<String, HourlyJobTotals>>>();
    	
        try (SmfRecordReader reader = 
                args.length == 0 ?
                SmfRecordReader.fromDD("INPUT") :
                SmfRecordReader.fromName(args[0])
                		.include(70,1)
                		.include(30,2)
                		.include(30,3))
        { 
            for (SmfRecord record : reader)                                                     
            {
            	switch (record.recordType())
            	{
            	case 70:
            		Smf70Record r70 = new Smf70Record(record);
            		systemHourLAC
            			.computeIfAbsent(r70.system(), system -> new HashMap<LocalDateTime, HourlyLac>())
            			.computeIfAbsent(r70.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HourlyLac())
            			.add(r70);
	            	break;
            	case 30:	
	            		Smf30Record r30 = new Smf30Record(record);
	            		systemHourJobnameTotals
	            			.computeIfAbsent(r30.system(), system -> new TreeMap<LocalDateTime, Map<String, HourlyJobTotals>>())
	            			.computeIfAbsent(r30.smfDateTime().truncatedTo(ChronoUnit.HOURS), time -> new HashMap<String, HourlyJobTotals>())
	            			.computeIfAbsent(r30.identificationSection().smf30jbn(), 
	            					jobname -> new HourlyJobTotals(r30.identificationSection().smf30jbn()))            			
	            			.add(r30);
	            	break;
	            default:
	            	break;
            	}
            }
        }
        createReport(systemHourLAC, systemHourJobnameTotals);
    }

	private static void createReport(
			Map<String, Map<LocalDateTime, HourlyLac>> systemHourLAC,
			Map<String, Map<LocalDateTime, Map<String, HourlyJobTotals>>> systemHourJobnameTotals) 
	{
		systemHourLAC.entrySet().stream()
        	.sorted((systemAinfo, systemBinfo) -> systemAinfo.getKey().compareTo(systemBinfo.getKey()))
        	.forEach(systemEntry ->
        	{
        		String system = systemEntry.getKey();
        		System.out.format("%n%n%s%n", system);
      		
        		systemEntry.getValue().entrySet().stream()
        			.sorted((hourA,hourB) // reversed to sort descending 
        					-> Long.compare(hourB.getValue().hourAverage(), hourA.getValue().hourAverage()))
        			.limit(5)
        			.forEachOrdered(hourEntry -> 
        			{
        				LocalDateTime hour = hourEntry.getKey();
        				long fourHourMSU = hourEntry.getValue().hourAverage();
        				System.out.println("");
        				System.out.println("    Hour                         4H MSU");
    				
                		System.out.format("    %10s %8s %15d%n", 
                				hour.format(DateTimeFormatter.ISO_LOCAL_DATE),
                				hour.format(DateTimeFormatter.ISO_LOCAL_TIME),
                				fourHourMSU);          		                		
                		reportFourHourTopJobs(hour, fourHourMSU, systemHourJobnameTotals.get(system));
        			});
        	}
        	);
	}

	private static void reportFourHourTopJobs(
			LocalDateTime hour,
			long msuvalue,
			Map<LocalDateTime, Map<String, HourlyJobTotals>> hourJobnameTotals) 
	{		
		System.out.println("");
		System.out.println("        Jobname       CPU%     Est. MSU");

		List<HourlyJobTotals> fourHourJobs = new ArrayList<>();
		for (int i=0; i < 4; i++) 
	    {  
		    fourHourJobs.addAll(
		            hourJobnameTotals.getOrDefault(hour.minusHours(i), Collections.emptyMap())
		            .values());
	    }
				
		double fourHourTotal = fourHourJobs.stream()
				.collect(Collectors.summingDouble(HourlyJobTotals::getCpu));
		
		fourHourJobs.stream()
			.collect(
				Collectors.groupingBy(HourlyJobTotals::getJobname, 
						Collectors.summingDouble(HourlyJobTotals::getCpu)))
			.entrySet().stream()
			.sorted((jobATotal,jobBTotal) -> jobBTotal.getValue().compareTo(jobATotal.getValue()))
			.limit(5)
			.forEachOrdered(jobTotal ->                		
				System.out.format("        %-12s %4.1f%% %12.1f%n", 
						jobTotal.getKey(), 
						jobTotal.getValue() / Duration.ofHours(4).getSeconds() * 100,
						jobTotal.getValue() / fourHourTotal * msuvalue));
	}

    private static class HourlyJobTotals
    {
    	public HourlyJobTotals(String jobname)
    	{
    		Jobname = jobname;
    	}
    	public void add(Smf30Record r30)
    	{
    		for (ProcessorAccountingSection pacct : r30.processorAccountingSections())
    		{
    			cptime = cptime
    				+ pacct.smf30cptSeconds()
    				+ pacct.smf30cpsSeconds()
    	    		+ pacct.smf30icuSeconds()
    	    	    + pacct.smf30isbSeconds()
    	    	    + pacct.smf30iipSeconds()
    	    	    + pacct.smf30rctSeconds()
    	    	    + pacct.smf30hptSeconds()
    				;
    		}
    	}
    	
    	public double getCpu()
    	{
    		return cptime;
    	}
    	
    	public String getJobname()
    	{
    		return Jobname;
    	}
    	
    	private double cptime = 0;
    	private String Jobname;
    }
    
    private static class HourlyLac
    {
    	// average is weighted by the number of samples in case of
    	// short RMF intervals
    	public void add(Smf70Record r70)
    	{
    		smf70lac += r70.cpuControlSection().smf70lac() * r70.productSection().smf70sam();
    		smf70sam += r70.productSection().smf70sam();
    	}
    	
    	public long hourAverage()
    	{
    		return smf70sam > 0 ? smf70lac / smf70sam : 0;
    	}
    	
    	private long smf70lac = 0;
    	private long smf70sam = 0;
    }
    
    
}
