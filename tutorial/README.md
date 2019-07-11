# EasySMF:JE Tutorial

These tutorials are provided to illustrate the use of the EasySMF:JE Java API to process SMF data.

Most samples are self-contained in a single file and use the default package to make compiling and running the code
on z/OS as simple as possible. You can transfer (or even cut and paste) the samples to a unix file on z/OS and
use the supplied JCL to compile and run the code.

### Java Rules

Java is strict about the naming of files. A class called **sample1** must be in a file called **sample1.java**.
It will be compiled to a file called **sample1.class**. Class names and file names are case sensitive.

To avoid name clashes, Java classes are usually contained in packages. **sample1** might be placed in package
**com.blackhillsoftware.samples**:

```
package com.blackhillsoftware.samples;
public class sample1 
{
   ...
}
```

The full class name becomes **com.blackhillsoftware.samples.sample1**. The source file needs to be
**com/blackhillsoftware/samples/sample1.java** and it will be compiled to file
**com/blackhillsoftware/samples/sample1.class**.

The samples in this tutorial use the **default package** i.e. no package. This means you don't have to search through 
subdirectories to find the code. It simplifies compiling and running the tutorials but is not recommended for
production code. 

## JCL

JCL is supplied to compile and run the code on z/OS.

There are jobs for both the [JZOS Batch Launcher](./JCL/JZOS.txt) and [BPXBATCH](./JCL/BPXBATCH.txt). 

The JZOS Batch Launcher is recommended because it allows the Java program to use regular z/OS JCL, with DD statements
to define input and output files. BPXBATCH runs Java under a Unix shell, which means you do not have access to most
DDs defined in the JCL.

The supplied JCL assumes that the samples are in a subdirectoy called **easysmf-samples** under your Unix home
directory. Source code is in **easysmf-samples/src** and the java compiler creates class files in
**easysmf-samples/target**.


## Sample 1: Read, extract and print SMF data

[Sample 1 Source Code: sample1.java](./src/sample1.java)

Sample 1 shows the basics of reading SMF data and extracting sections and fields.

Various CPU times are extracted and printed from the Processor Accounting section in the SMF type 30
subtype 5 (Job End) records. The data is printed in CSV format.

#### Reading SMF Data

SMF Records are read using the SmfRecordReader class. This class implements AutoCloseable and should be
used in a try-with-resources block so it is automaticaly closed before the program exits.

The samples are set up so that they can run as a z/OS batch job under the JZOS Batch Launcher and read
from a DD, or on another platform (Windows, Linux, or z/OS BPXBATCH) with the input file name passed
as a command line argument to the program.

If there are no command line arguments, the program attempts to open the DD **INPUT**. If there are command
line arguments, the first argument is used as a file name for the SmfRecordReader.  
   

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

Process each record. Check whether it has a Processor Accounting Section (some records don't e.g. when
a job has more data than will fit in a single SMF record) and if found print information about the job.   

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

- Methods are provided to extract the specific SMF sections e.g. `identificationSection()`,
`processorAccountingSection()`. If there may be more than one section the method returns a List of
sections, e.g. `excpSections()` returns `List<ExcpSection>`. 
- Methods that return a list return an **empty list** if there a none of that section in the record.
This means you can iterate over the lists without specifically checking whether sections are present - an
empty list simply interates 0 times.
- Records and sections have methods to extract the specific fields.
  - Text values return a String e.g. `smf30jbn()`
  - Numeric values return either a 32 bit **int** value (for unsigned fields less than 4 bytes and signed
  fields up to 4 bytes) or a 64 bit **long** value for 4-8 byte unsigned fields and 5-8 byte signed fields.
  Unsigned 8 byte fields will throw an exception if the value exceeds the maximum for a signed 64 bit value.
  Unsigned 8 byte field values are also available as a **BigInteger**. Use the BigInteger value if the value
  can exceed the maximum signed 64 bit value. 
  - Values representing an amount of time e.g. a CPU time are available both as a **java.time.Duration** and
  converted to seconds as a floating point **double** value. e.g. `Duration smf30cpt()` and `double smf30cptSeconds()`.
  - Time values may be converted to **java.time.LocalTime**, **java.time.LocalDate**, **java.time.LocalDateTime**
  or **java.time.ZonedDateTime** (typically with ZoneOffset.UTC) depending on the type of value they represent.
  This prevents e.g. accidentally comparing local time fields with UTC time fields.
  You can convert a LocalDateTime to a ZonedDateTime by applying a time zone. You can then validly compare these 
  fields with UTC values, and even compare times from systems running with different time zones by specifying
  the correct timezone for each system. 
- The objective is to provide consistent methods and values across all SMF record types, so that techniques
  for accessing the data are transferable from one record type to another.
- Data about the meaning of the fields can be found in the various IBM documentation. Documenting the meaning
  of the fields is beyond the scope of the product.
 

## Sample 2: Filter and print SMF data

[Sample 2 Source Code: sample2.java](./src/sample2.java)

Sample 2 shows how you can use Java Streams to filter and process SMF data.

The sample searches for SMF type 14 (input) and 15 (output) dataset close records for dataset SYS1.PARMLIB, and
prints the time, system, jobname and the type of access.

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

This sequence tells the SmfRecordReader to include SMF type 14 and type 15 records, then streams the records
to the next steps.
- **map** creates a Smf14Record from the base SmfRecord (type 14 and 15 records have the same format).
- **filter** passes only the records that match the filter criteria: smfjfcb1() is the JFCB, which contains the
  dataset name in jfcbdsnm(). We want to list references to SYS1.PARMLIB.
- **limit** stop after this number of matches.
- **forEach** process each record. In this case, print the output.

## Sample 3: Search SMF for a Text String 

[Sample 3 Source Code: sample3.java](./src/sample3.java)

Sample 3 shows how you can search for specific text when you don't know which specific record types might be relevant.

It is similar to sample 2, except that the processing is done on the base SMF record.

Filtering is applied to ignore types 14, 15 and 42 subtype 6 records because we already know the dataset name will
be found in those records.

For each match we write a header with the time, system, record type and subtype then dump the record. We stop
after 100 matches to avoid excessive output.

#### Notes

The entire record is translated to a Java string before searching for the text. For performance reasons it is best
to do all other filtering e.g. record types before the string search - this avoids doing the translation unnecessarily.

When running this sample, start with a limited amount of data and monitor the CPU time consumed. In testing,
the program used about 10 seconds of CPU time per GB of SMF data on the IBM Dallas Remote Development system. 90%
of that was on a zIIP.

## Sample 4: Group and Summarize SMF data

[Sample 4 Source Code: sample4.java](./src/sample4.java)

Sample 4 shows how group and summarize SMF data in Java.

The program reports statistics for each program name, taken from SMF type 30 subtype 4 (Step End) records. Data does
not need to be sorted, and the program information is collected in a `java.util.HashMap<>`. This means that CPU usage
will scale approximately linearly with the amount of data processed, and memory requirements will depend on the number
of different program names encountered.

Statistics for each program are collected in a class called **ProgramStatistics**. There is an instance of the class
for each program, kept in the HashMap with the program name as the key.

```
Map<String, ProgramStatistics> programs = new HashMap<String, ProgramStatistics>();
```

We read and process the records the same way as in sample1.

We use the Map.computeIfAbsent() method to get the ProgramStatistics entry for the program name. It checks whether the
key is present in the map. If it is present, the existing entry is returned. If it is not present, a new instance
is created, added to the map with the corresponding key, and returned. 

```
ProgramStatistics program = programs
   .computeIfAbsent(
      r30.identificationSection().smf30pgm(), // program name
      x -> new ProgramStatistics());
```
We then accumulate the information from the record into the ProgramStatistics entry:
```
program.accumulateData(r30);
```


```
private static class ProgramStatistics
{
    ...
    public void accumulateData(Smf30Record r30)
    {        	
        if (r30.processorAccountingSection() != null)
        {
            count++;
            cpTime += r30.processorAccountingSection().smf30cptSeconds()
                    + r30.processorAccountingSection().smf30cpsSeconds();
            ziipTime += r30.processorAccountingSection().smf30TimeOnZiipSeconds();   
            normalizedZiipTime += 
                r30.processorAccountingSection().smf30TimeOnZiipSeconds() 
                    * r30.performanceSection().smf30snf() / 256;
        }
        if (r30.ioActivitySection() != null)
        {
            excps += r30.ioActivitySection().smf30tex();
            connectTime += r30.ioActivitySection().smf30aicSeconds();
        }
    }
    ...
}
```

We use Java Streams again to write the report:
```
programs.entrySet().stream()
    .sorted((a, b) -> Double.compare(b.getValue().cpTime, a.getValue().cpTime))
    .limit(100)
    .forEachOrdered(program ->
    {
        ProgramStatistics programinfo = program.getValue();
        System.out.format("%-8s %,8d %14s %14s %14s %,14d %14s %14s %14s %,14d %14.3f%n", 
            program.getKey(),
            programinfo.count, 
            ...
    });
```
- stream the entries from the map,
- sort by total CP Time (reversing a and b to sort descending)
- take the first 100 entries
- print the statistics.

**forEachOrdered(...)** guarantees that the order of the entries is maintained - which is important after the sort. 
**forEach** does not guarantee that the order is maintained - although in simple cases it seems to be.  

The principles used in this sample can be adapted to many different situations, simply by changing the data
accumulated in the Statistics class and the data used for the key in the Map. 
  
## Sample 5: Accessing Sections in Lists

When a SMF record may have multiple instances of a section, the sections will be returned in a `List<T>`. Sometimes a record will have no instances of a particular section - in that case an empty list is returned. 

Sample 5 generates a report based on the SMF type 30 EXCP Section.

The report lists job steps with a STEPLIB entry in the EXCP sections. The program ignores jobs where the job name begins with the userid. Each combination of Jobname, Step Number, Step Name and Program Name is listed only once.

(This sample is used to demonstrate various techniques. It is not intended to imply that there is any problem with jobs using a STEPLIB.)

#### Eliminating Duplicates

To eliminate duplicates, we collect the entries in a `Set<T>`. A Set is a collection which does not allow duplicate entries. We need to create a class to contain the job/step/program information, and provide `hashCode()` and `equals(Object)` methods which the Set will use to test for equality.

```
private static class JobStepProgram
{	        
    private String jobname;
    private int stepnumber;
    private String stepname;
    private String programname;      
...
    @Override
    public int hashCode() {
        return Objects.hash(jobname, programname, stepname, stepnumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JobStepProgram other = (JobStepProgram) obj;
        return Objects.equals(jobname, other.jobname)
            && Objects.equals(programname, other.programname) 
            && Objects.equals(stepname, other.stepname)
            && stepnumber == other.stepnumber;
    }
...
}
```     

##### A short discussion on hashcode() and equals(Object) #####

The `equals(Object obj)` method needs to compare all the properties of the object (jobname, step number, step name and program name) that determine whether the objects are equal.

The most important rule for `hashcode()` is that objects that are equal must return the same hash code. Otherwise collections like HashSet and HashMap will not work correctly. This means that if you provide an equals() method, you **must** also provide a hashcode() method.

The second rule is that objects that are **not** equal **should** return different hash codes. It is not absolutely required (and not even possible for every instance of every object) but if hash collisions are frequent it can badly affect performance of the hash based collections.

The methods shown were generated by Eclipse. Equals compares the fields in the class. The Objects.hash(...) method combines hashes from the same fields into a new hash using a method that should be resistant to collisions.

This is a bare bones discussion of hashcode() and equals(), there is plenty of Java books and documentation that goes into greater depth.
 

## Sample 6: A/B Reporting