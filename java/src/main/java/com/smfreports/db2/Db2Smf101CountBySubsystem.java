package com.smfreports.db2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.blackhillsoftware.smf.SmfRecord;
import com.blackhillsoftware.smf.SmfRecordReader;
import com.blackhillsoftware.smf.db2.Smf101Record;

/**
 * 
 * Count the number of DB2 101 records by DB2 subsystem (field SM101SSI)
 *
 */

public class Db2Smf101CountBySubsystem {

    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            System.out.println("Usage: Db2Smf101CountBySubsystem <input-name>");
            System.out.println("<input-name> can be filename, //DD:DDNAME or //'DATASET.NAME'");          
            return;
        }
    	
        Map<String, SubsystemInfo> subsystems = new HashMap<String, SubsystemInfo>();
        
        // SmfRecordReader.fromName(...) accepts a filename, a DD name in the
        // format //DD:DDNAME or MVS dataset name in the form //'DATASET.NAME'
    	
        try (SmfRecordReader reader = SmfRecordReader.fromName(args[0]))                
        {
        	
        	reader.include(101);
        	
			for (SmfRecord r : reader) {
				Smf101Record r101 = new Smf101Record(r);
				subsystems.computeIfAbsent(r101.sm101ssi(),	name -> new SubsystemInfo(name))
					.add(r101);
			}
			
        }
        subsystems.values().stream()
	        .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
	        .forEachOrdered(subsystem -> 
	        	System.out.format("%-4s %8d%n", subsystem.getName(), subsystem.getCount()));
        
        System.out.println("Finished");
    }
    
	private static class SubsystemInfo {
		private String name;
		private int count = 0;

		SubsystemInfo(String name) {
			this.name = name;
		}

		void add(Smf101Record record) {
			count++;
		}

		String getName() {
			return name;
		}

		int getCount() {
			return count;
		}
	}
}

