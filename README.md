# easysmf-samples

Sample code for EasySMF

### Summarizing Data ###

This report shows how to create a summary report. The report summarizes various job statistics from type 30 records by Job Name.

[Jobs by Job Name](./java/src/main/java/com/smfreports/jobsummary/JobsByJobname.java)

### A generic class to summarize job data ###

The summary code can be extracted into a separate class and then used to summarize job data by a number of different criteria.

The function to extract the specific field to use for the summary is passed as a parameter to the report method.

[JobSummarizer](./java/src/main/java/com/smfreports/genericjobsummary/JobSummarizer.java)

#### Classes to call the job summaries ####

These programs use SMF 30 subtype 5 records to summarize data at the job level by Userid and Job Class.

[Jobs by Userid](./java/src/main/java/com/smfreports/genericjobsummary/JobsByUserid.java)

[Jobs by Job Class](./java/src/main/java/com/smfreports/genericjobsummary/JobsByJobClass.java)

Steps by Program Name uses SMF 30 subtype 4 to gather information by Program Name from the step end records.

[Steps by Program Name](./java/src/main/java/com/smfreports/genericjobsummary/StepsByProgramName.java)