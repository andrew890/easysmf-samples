import java.io.*;

import com.blackhillsoftware.smf.*;
import com.blackhillsoftware.smf.cics.Smf110Record;

public class GenerateInput
{
    private static void printUsage() {
        System.out.println("Usage: GenerateInput <count> <input-file> <output-file>");
        System.out.println("");
        System.out.println("Copy records from input-file to output-file, repeating until output-file contains the specified number of transactions.");
        System.out.println("");
        System.out.println("  count        The target number of transactions");
        System.out.println("  input-file   File containing SMF records. Binary data, RECFM=U or V[B]");
        System.out.println("               including RDW.");
        System.out.println("  output-file  Output-file for records.");
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length < 3 || args[0].equals("--help") || args[0].equals("-h"))
        {
            printUsage();
            System.exit(0);
        }

        try (SmfRecordWriter writer = SmfRecordWriter.fromName(args[2]))
        {
            long target = Long.parseLong(args[0]);
            long count = 0;
            long in = 0;
            long out = 0;
            
            while (count < target)
            {
                try (SmfRecordReader reader = SmfRecordReader.fromName(args[1])
                        .include(110, 1))
                {
                    for (SmfRecord record : reader)
                    {
                        in++;
                        Smf110Record r110 = Smf110Record.from(record);
                        if (r110.performanceRecords().size() > 0)
                        {
                            count += r110.performanceRecords().size();
                            writer.write(record);
                            out++;
                            if (count >= target) break;
                        }
                    }
                }
                if (count == 0)
                {
                	throw new RuntimeException("No transaction records in input file!");                            
                }
                System.out.format("%d transactions...%n", count);
            }

            System.out.format("Finished, %d records in, %d records out, %d transactions.%n", in, out, count);
        }        
        catch (Exception e)
        {
            printUsage();
            throw e;
        }
    }   
}
