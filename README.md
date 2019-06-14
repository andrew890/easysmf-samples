# easysmf-samples

This repository contains sample code for EasySMF:JE.

EasySMF:JE is a commercial product developed by [Black Hill Software](https://www.blackhillsoftware.com) which provides a Java API to map z/OS SMF records. To run these samples, you will require the EasySMF:JE jar file and a EasySMF:JE license key.

A free, 30 day trial is available. You can request a trial here: [EasySMF 30 Day Trial](https://www.blackhillsoftware.com/30-day-trial/)

## Sample SMF Reports

Various reports are provided to demonstrate how to use EasySMF:JE.

### Summarizing Data ###

This report shows how to create a summary report. The report summarizes various job statistics from type 30 records by Job Name.

[Jobs by Job Name](./java/src/main/java/com/smfreports/jobsummary/JobsByJobname.java)

### A generic class to summarize job data ###

The summary code can be extracted into a separate class and then used to summarize job data by a number of different criteria.

The function to extract the specific field to use for the summary is passed as a parameter to the report method.

[JobGroupReport](./java/src/main/java/com/smfreports/genericjobsummary/JobGroupReport.java)

#### Classes to call the job summaries ####

These programs use SMF 30 subtype 5 records to summarize data at the job level by Userid and Job Class.

[Jobs by Userid](./java/src/main/java/com/smfreports/genericjobsummary/JobsByUserid.java)

[Jobs by Job Class](./java/src/main/java/com/smfreports/genericjobsummary/JobsByJobClass.java)

Steps by Program Name uses SMF 30 subtype 4 to summarize information by Program Name from the step end records.

[Steps by Program Name](./java/src/main/java/com/smfreports/genericjobsummary/StepsByProgramName.java)

Interval by Userid uses SMF 30 subtype 2 and 3 to summarize information by Userid from the interval records.
It shows usage for the time period covered by the SMF data, rather than all usage for jobs that ended in the
time period.

[Interval by Userid](./java/src/main/java/com/smfreports/genericjobsummary/IntervalByUserid.java.java)
