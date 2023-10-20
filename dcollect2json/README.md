# Convert DCOLLECT records to JSON format

This sample generates JSON from DCOLLECT records.

Each DCOLLECT record type is written to a different DD name. Select which record types you want by including or omitting the output DD names in the JCL.

The sample must run under the JZOS batch launcher so that it has access to the DDs in the JCL.

See the [JCL here](./JCL/DC2JSON.txt)