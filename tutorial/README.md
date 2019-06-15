# EasySMF:JE Tutorial

These tutorials are provided to illustrate the use of the EasySMF:JE Java API to process SMF data.

Most samples are self-contained in a single file and use the default package to make compiling and running the code on z/OS as simple as possible. You can transfer (or even cut and paste) the samples to a unix file on z/OS and use the supplied JCL to compile and run the code.

### Java Rules

Java is strict about the naming of files. A class called **sample1** must be in a file called **sample1.java**. It will be compiled to a file called **sample1.class**. Class names and file names are case sensitive.

To avoid name clashes, Java classes are usually contained in packages. **sample1** might be placed in package **com.blackhillsoftware.samples**:

```
package com.blackhillsoftware.samples;
public class sample1 
{
   ...
}
```

The full class name becomes **com.blackhillsoftware.samples.sample1**. The source file needs to be **com/blackhillsoftware/samples/sample1.java** and it will be compiled to file **com/blackhillsoftware/samples/sample1.class**.

The samples in this tutorial use the **default package** i.e. no package. This means you don't have to search through subdirectories to find the code. It simplifies the tutorials but is not recommended for prodution code. 

## JCL

JCL is supplied to compile and run the code on z/OS.

There are jobs for both the JZOS Batch Launcher and BPXBATCH. The JZOS Batch Launcher is recommended because it allows the Java program to use regular z/OS JCL, with DD statements to define input and output files. BPXBATCH runs Java under a Unix shell, which means you do not have access to most DDs defined in the JCL.

The supplied JCL assumes that the samples are in a subdirectoy called **easysmf-samples** under your Unix home directory. Source code is in **easysmf-samples/src** and the java compiler creates class files in **easysmf-samples/target**.


## Sample 1: Read, extract and print SMF data

[Source: sample1.java](./src/sample1.java)

Sample 1 shows the basics of reading SMF data and extracting sections and fields.

Various CPU times are extracted and printed from the Processor Accounting section in the SMF type 30 subtype 5 (Job End) records. The data is printed in CSV format.

#### Reading SMF Data

SMF Records are read using the SmfRecordReader class. This class implements AutoCloseable and should be used in a try-with-resources block so it is automaticaly closed before the program exits.

The samples are set up so that they can run as a z/OS batch job under the JZOS Batch Launcher and read from a DD, or on another platform (Windows, Linux, or z/OS BPXBATCH) with the input file name passed as a command line argument to the program.

If there are no command line arguments, the program attempts to open the DD **INPUT**. If there are command line arguments, the first argument is used as a file name for the SmfRecordReader.  
   

```
try (SmfRecordReader reader = 
         args.length == 0 ?
              SmfRecordReader.fromDD("INPUT") :
              SmfRecordReader.fromName(args[0])) 
{
    ...                                                                          
}
```

We tell the SmfRecordReader to include only type 30 subtype 5 records: 

```
reader.include(30,5);
```

#### Processing the Data

Read the records and create a Smf30Record object from each base SmfRecord:

```
for (SmfRecord record : reader)
{
    Smf30Record r30 = Smf30Record.from(record);
    ...
} 

```

Process each record. Check whether it has a Processor Accounting Section (some records don't e.g. when a job has more data than will fit in a single SMF record) and if found print information about the job.   

```
if (r30.processorAccountingSection() != null)
{
    System.out.format("%s,%s,%s,%s,%.2f,%.2f,%.2f%n",                                  
        r30.smfDateTime(), 
        r30.system(),
        r30.identificationSection().smf30jbn(),
        r30.identificationSection().smf30jnm(),
        r30.processorAccountingSection().smf30cptSeconds()
            + r30.processorAccountingSection().smf30cpsSeconds(),
        r30.processorAccountingSection().smf30TimeOnZiipSeconds(),
        r30.processorAccountingSection().smf30TimeZiipOnCpSeconds()
        );
}
```

#### Notes

- Methods are provided to extract the specific SMF sections e.g. `identificationSection()`, `processorAccountingSection()`. If there may be more than one section the method returns a List of sections, e.g. `excpSections()` returns  
`List<ExcpSection>`. 
- Methods that return a list return an **empty list** if there a none of that section in the record. This means you can iterate over the lists without specifically checking whether sections are present - an empty list simply interates 0 times.
- Records and sections have methods to extract the specific fields.
  - Text values return a String e.g. `smf30jbn()`
  - Numeric values return either a 32 bit **int** value (for unsigned fields less than 4 bytes and signed fields up to 4 bytes) or a 64 bit **long** value for 4-8 byte unsigned fields and 5-8 byte signed fields.
  Unsigned 8 byte fields will throw an exception if the value exceeds the maximum for a signed 64 bit value. Unsigned 8 byte field values are also available as a **BigInteger**. Use the BigInteger value if the value can exceed the maximum signed 64 bit value. 
  - Values representing an amount of time e.g. a CPU time are available both as a **java.time.Duration** and converted to seconds as a floating point **double** value. e.g. `Duration smf30cpt()` and  `double smf30cptSeconds()`.
  - Time values may be converted to **java.time.LocalTime**, **java.time.LocalDate**, **java.time.LocalDateTime** or **java.time.ZonedDateTime** (typically with ZoneOffset.UTC) depending on the type of value they represent. This prevents e.g. accidentally comparing local time fields with UTC time fields.
  You can convert a LocalDateTime to a ZonedDateTime by applying a time zone. You can then validly compare these fields with UTC values, and even compare times from systems running with different time zones by specifying the correct timezone for each system. 
- The objective is to provide consistent methods and values across all SMF record types, so that techniques for accessing the data are transferable from one record type to another.
- Data about the meaning of the fields can be found in the various IBM documentation. Documenting the meaning of the fields is beyond the scope of the product.
 

## Sample 2: Filter and print SMF data

[Source: sample2.java](./src/sample2.java)

Sample 2 shows how you can use Java Streams to filter and process SMF data.

The sample searches for SMF type 14 (input) and 15 (output) dataset close records for dataset SYS1.PARMLIB, and prints the time, system, jobname and the type of access.

The SmfRecordReader is opened in the same was as for sample1.

The Stream API involves a sequence of steps, where the output of each step is passed to the next step.

```
reader
    .include(14)            	
    .include(15)
    .stream()
    .map(record -> Smf14Record.from(record)) 
    .filter(r14 -> r14.smfjfcb1().jfcbdsnm().equals("SYS1.PARMLIB"))
    .limit(1000000)
    .forEach(r14 -> 
        System.out.format("%-23s %-4s %-8s %-6s%n",                                  
        r14.smfDateTime(), 
        r14.system(),
        r14.smf14jbn(),
        r14.smf14rty() == 14 ? "Read" : "Update"));    
```

This sequence tells the SmfRecordReader to include SMF type 14 and type 15 records, then streams the records to the next steps.
- **map** creates a Smf14Record from the base SmfRecord (type 14 and 15 records have the same format).
- **filter** passes only the records that match the filter criteria: smfjfcb1() is the JFCB, which contains the dataset name in jfcbdsnm(). We want to list references to SYS1.PARMLIB.
- **limit** stop after this number of matches.
- **forEach** process each record. In this case, print the output.


## Sample 3: Search SMF for a Text String 

## Sample 4: Group and Summarize SMF data

## Sample 5: Advanced Grouping