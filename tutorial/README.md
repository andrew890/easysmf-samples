# Tutorial : Processing SMF Data using Java

These tutorials are provided to illustrate the use of the EasySMF:JE Java API to process SMF data.

## 1. [Introduction](#EasySMF:JE-Introduction)
## 1. [JCL](#JCL)
## 1. [Sample Code](#Samples)
      1. [Read, extract and print SMF data](Sample-1:-Read,-extract-and-print-SMF-data)
      2. Filter and print SMF data
      3. Search SMF for a Text String
      4. Group and Summarize SMF data
      5. Repeating Record Sections
      6. A/B Reporting
 

## EasySMF:JE Introduction

EasySMF:JE provides an API for accessing SMF data using Java. It provides Java classes to map SMF records and SMF record sections.

The Java classes provide access to the data in the record, without needing to understand the underlying record structure.
The classes provide interfaces that are as consistent as possible across all the different record types.
Techniques learned for one record type are easily transferrable to other record types.

### Data Conversion

Data in SMF record fields are converted to Java types using a consistent set of principles.

#### Dates and Times

- Dates and times are converted to [java.time](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) classes, depending on the type of data they contain. 
They could be a LocalDate, LocalTime, LocalDateTime or a ZonedDateTime with ZoneOffset.UTC. 
- Quantities of time e.g. CPU time, connect time are available as a Duration and as a floating point value in seconds.
- The raw (unconverted) value is also available.

EasySMF time conversions mean that you do not need to worry about the units for the different fields when you are processing SMF data.
Different data types for local and UTC date/times provide a defense against programming errors like comparing local and UTC time fields.
Java has built in time zone support, so you can transform times between UTC and local and between different time zones according to the rules of your system time zone(s).

Java.time classes store values with nanosecond precision. This is more than adequate for most SMF fields. Where more precision is required e.g. STCK timestamps, you can choose to use the raw value to get the full precision.

#### Numeric values

- Binary fields up to 3 bytes unsigned or 4 bytes signed are converted to Java **int**
- Binary fields of 4-8 bytes are converted to Java **long**.
8 byte unsigned values are also available as **BigInteger** values. They will throw an exception if the value exceeds the maximum value for a signed long, i.e. if the high order bit is set. If this is possible you should use the BigInteger value.
- Floating point values are converted to Java **double** values.
- Packed decimal data is converted to **int**, **long** or **BigInteger** depending on the number of digits.

#### Text Values

Text data e.g. EBCDIC data is converted to a Java **String**. Some records contain UTF8 data, this is also extracted into a Java String.

### SMF Sections

Classes mapping SMF records and sections provide methods to access the SMF sections and sub-sections. For example, the Smf30Record class has a [completionSection()](https://static.blackhillsoftware.com/easysmf/javadoc/com/blackhillsoftware/smf/smf30/Smf30Record.html#completionSection--) method to access the SMF 30 Completion Section.

If there is no Completion Section in the record completionSection() returns null.

#### Multiple Sections

Often a section can occur multiple times in a record e.g. sections located by a SMF **triplet**. In that case the sections are returned in a **List**.

An example is the [excpSections()](https://static.blackhillsoftware.com/easysmf/javadoc/com/blackhillsoftware/smf/smf30/Smf30Record.html#excpSections--) method in the Smf30Record.

 If the record does not contain any of that particular section i.e. the triplet has zero for the count the method returns an empty list.

Frequently this means that you don't need a special check whether the section is present, iterators will simply iterate zero times for an empty list.

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

The samples in this tutorial use the **default package** i.e. no package.
This means you don't have to search through subdirectories to find the code.
It simplifies compiling and running the tutorials but is not recommended for production code.

## JCL

JCL is supplied to compile and run the code on z/OS.

There are jobs for both the [JZOS Batch Launcher](./JCL/JZOS.txt) and [BPXBATCH](./JCL/BPXBATCH.txt). 

The JZOS Batch Launcher is recommended because it allows the Java program to use regular z/OS JCL, with DD statements
to define input and output files. BPXBATCH runs Java under a Unix shell, which means you do not have access to most
DDs defined in the JCL.

The supplied JCL assumes that the samples are in a subdirectoy called **easysmf-samples** under your Unix home
directory. Source code is in **easysmf-samples/src** and the java compiler creates class files in
**easysmf-samples/target**.

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
