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


## [Sample 1: Read, extract and print SMF data](Sample1.md)

Sample 1 shows the basics of reading SMF data and extracting sections and fields.

Various CPU times are extracted and printed from the Processor Accounting section in the SMF type 30
subtype 5 (Job End) records. The data is printed in CSV format.

## [Sample 2: Filter and print SMF data](Sample2.md)

Sample 2 shows how you can use Java Streams to filter and process SMF data.

The sample searches for SMF type 14 (input) and 15 (output) dataset close records for dataset SYS1.PARMLIB, and
prints the time, system, jobname and the type of access.

## [Sample 3: Search SMF for a Text String](Sample3.md)

[Sample 3 Source Code: sample3.java](./src/sample3.java)

Sample 3 shows how you can search for specific text when you don't know which specific record types might be relevant.

## [Sample 4: Group and Summarize SMF data](Sample4.md)

[Sample 4 Source Code: sample4.java](./src/sample4.java)

Sample 4 shows how group and summarize SMF data in Java.

The program reports statistics for each program name, taken from SMF type 30 subtype 4 (Step End) records.

## [Sample 5: Accessing Sections in Lists](Sample5.md)

When a SMF record may have multiple instances of a section, the sections will be returned in a `List<T>`. Sometimes a record will have no instances of a particular section - in that case an empty list is returned. 

Sample 5 generates a report based on the SMF type 30 EXCP Section.

## [Sample 6: A/B Reporting](Sample6.md)