# Dataset Reports

These reports are based on varios SMF records showing activity against datasets.

### SmfSearch

A simple program to demonstrate searching SMF data.

This program searches SMF type 15 (Output Dataset Activity) records for a dataset name. The dataset name and SMF input source are passed as arguments to the program.

### DatasetActivity

A more complex report of dataset activity. The report shows activity from SMF record types:
- 14 - Read
- 15 - Update
- 17 - Scratch
- 18 - Rename
- 61 - ICF Define
- 62 - VSAM Open
- 64 - VSAM Status
- 65 - ICF Delete

#### Arguments

 - -r : Optional, indicates that read activity should be included. Otherwise only datasets opened for write access are included.
 - dataset-pattern : The datasets to be reported.
 - input-name : The source of the input data. Can be **filename**, **//DD:DDNAME** or **//'DATASET.NAME'** 

Wildcards can be used in **dataset-pattern**:
- % - represents a single character in a qualifier
- \* - Zero or more characters in a single qualifier
- \*\* - Zero or more characters in one or more qualifiers 

e.g Read or write activity against SYS1.PARMLIB:
```
java com.smfreports.dataset.DatasetActivity -r SYS1.PARMLIB //'SMF.DUMP.DAILY'
```
or for update activity for datasets starting with "SYS" and ending with ".*LIB":
```
java com.smfreports.dataset.DatasetActivity SYS**.*LIB //'SMF.DUMP.DAILY'
```

