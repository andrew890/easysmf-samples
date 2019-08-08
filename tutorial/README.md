# Tutorial : Processing SMF Data using Java

These tutorials are provided to illustrate the use of the EasySMF:JE Java API to process SMF data.

## [EasySMF:JE Introduction](Introduction.md)

EasySMF:JE provides an API for accessing SMF data using Java. It provides Java classes to map SMF records and SMF record sections.

The Java classes provide access to the data in the record, without needing to understand the underlying record structure.
The classes provide interfaces that are as consistent as possible across all the different record types.
Techniques learned for one record type are easily transferrable to other record types.

## [JCL](JCL.md)

JCL is supplied to compile and run the code on z/OS.

## Samples

Most samples are self-contained in a single file and use the default package to make compiling and running the code
on z/OS as simple as possible. You can transfer (or even cut and paste) the samples to a unix file on z/OS and
use the supplied JCL to compile and run the code.

### [Sample 1: Read, extract and print SMF data](Sample1.md)

Sample 1 shows the basics of reading SMF data and extracting sections and fields.

Various CPU times are extracted and printed from the Processor Accounting section in the SMF type 30
subtype 5 (Job End) records. The data is printed in CSV format.

### [Sample 2: Filter and print SMF data](Sample2.md)

Sample 2 shows how you can use Java Streams to filter and process SMF data.

The sample searches for SMF type 14 (input) and 15 (output) dataset close records for dataset SYS1.PARMLIB, and
prints the time, system, jobname and the type of access.

### [Sample 3: Search SMF for a Text String](Sample3.md)

Sample 3 shows how you can search for specific text when you don't know which specific record types might be relevant.

### [Sample 4: Group and Summarize SMF data](Sample4.md)

Sample 4 shows how group and summarize SMF data in Java.

The program reports statistics for each program name, taken from SMF type 30 subtype 4 (Step End) records.

### [Sample 5: Repeating Record Sections](Sample5.md)

When a SMF record may have multiple instances of a section, the sections will be returned in a `List<T>`. Sometimes a record will have no instances of a particular section - in that case an empty list is returned. 

Sample 5 generates a report based on the SMF type 30 EXCP Section.

### [Sample 6: A/B Reporting](Sample6.md)
