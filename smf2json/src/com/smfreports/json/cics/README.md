# Converting CICS SMF data to JSON format

These sample programs convert a number of different types of CICS SMF data to JSON format.
The JSON data can be processed further using other reporting tools that process JSON, e.g. Splunk.

All programs use the Smf2JsonCLI class, which provides a command line interface to handle reading SMF data and creating and writing JSON using the EasySMF-JSON functions.

Smf2JsonCLI can read SMF data from files or z/OS DD names. Output can be written to a file, to a z/OS DD or to stdout. The program can run on z/OS as a batch job using the JZOS batch launcher or BPXBATCH, or on any Java platform where the SMF data is available.

## Reports

The following sample reports are provided:

### CicsAbendTransactions

List detailed transaction information for transactions which abended 
(fields ABCODEC or ABCODEO contain data).

### CicsExceptions

Convert CICS Exception SMF records to JSON format.

### CicsSlowTransactions

List detailed transaction information for transactions with an elapsed time greater than a specified threshold.

### CicsStatistics

Convert CICS Statistics records to JSON format.

### CicsTransactionSummary

Create a minute by minute summary of CICS transaction data.

Data is grouped by a combination of the following fields:

* SMFMNPRN - Generic APPLID
* SMFMNSPN - Specific APPLID
* minute - Minute from the STOP time
* TRAN - Transaction identification
* TTYPE - Transaction start type
* RTYPE - Performance record type
* PGMNAME - Name of the first program
* SRVCLSNM - WLM service class name
* RPTCLSNM - WLM report class name
* TCLSNAME - Transaction class name

Fields can be added or deleted from the group key as required. The amount of output data increases based on the resulting number of groups, but the flexibility of the reporting also increases.

The intention is that the grouped data can be filtered and/or further grouped by JSON reporting tools to provide detailed minute by minute views of transaction data.




