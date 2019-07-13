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