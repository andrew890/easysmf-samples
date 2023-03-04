# Converting CICS SMF data to JSON format

These sample programs convert a number of different types of CICS SMF data to JSON format.
The JSON data can be processed further using other reporting tools that process JSON, e.g. Splunk.

All programs use the Smf2JsonCLI class, which provides a command line interface to handle reading SMF data and creating and writing JSON using the EasySMF-JSON functions.

Smf2JsonCLI can read SMF data from files or z/OS DD names. Output can be written to a file, to a z/OS DD or to stdout. The program can run on z/OS as a batch job using the JZOS batch launcher or BPXBATCH, or on any Java platform where the SMF data is available.

## CICS Dictionaries

CICS transaction reports require a dictionary to interpret the SMF records.

The CICS dictionary records need to be read before the transaction records. The simplest way to do that is to have a separate file/dataset with the dictionary records, and concatenate it ahead of the transaction data if using JCL or list the dictionary file before the transaction data file on the command line.

## Reports

The following sample reports are provided:

### CicsExceptions

Convert CICS Exception SMF records to JSON format.

### CicsTransactions

List detailed transaction information. Optionally select by

- Elapsed time greater than a specified value
- Transactions that abended (fields ABCODEC or ABCODEO contain data)

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

### CicsTransactionSummaryCustom

Similar to **CicsTransactionSummary**, except that transaction summary information is collected in a user provided *TransactionData* class instead of the supplied *CicsTransactionGroup* class.

The TransactionData class can be customized to control which data is collected. Collecting a smaller number of fields improves performance of the program.

## Running the Programs

JCL to run on z/OS can be found in the [JCL](./JCL) directory.

To run on Windows/Linux:

1) Build the Smf2Json project if required
2) Run the Java program, specifying the class as required:   

   ```
   java -cp target/* com.smfreports.json.cics.CicsTransactionSummary --out json.txt <input1> <input2> ...   
   ```
   
   where \<input1\> \<input2\> are SMF data files. For CICS transactions reports, CICS dictionary records must be read before the CICS transaction records.