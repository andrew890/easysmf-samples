## JCL

JCL is supplied to compile and run the code on z/OS.

There are jobs for both the [JZOS Batch Launcher](./JCL/JZOS.txt) and [BPXBATCH](./JCL/BPXBATCH.txt). 

The JZOS Batch Launcher is recommended because it allows the Java program to use regular z/OS JCL, with DD statements
to define input and output files. BPXBATCH runs Java under a Unix shell, which means you do not have access to most
DDs defined in the JCL.

The supplied JCL assumes that the samples are in a subdirectoy called **easysmf-samples** under your Unix home
directory. Source code is in **easysmf-samples/src** and the java compiler creates class files in
**easysmf-samples/target**.

