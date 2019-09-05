//JOBNAME  JOB CLASS=A,
//             MSGCLASS=H,
//             NOTIFY=&SYSUID
//*
//* ***** Edit with CAPS OFF and NUMBER OFF *****
//*
//* Compile a Java program using BPXBATCH
//*
// EXPORT SYMLIST=*
//*
//* File to compile
// SET CLASS='com/blackhillsoftware/samples/RecordCount.java'
//*
//* Java source and target directories.
//* As distributed, they are relative to user's home directory
//*
// SET SRC='./java/src'
// SET TGT='./java/target'
//*
//* EasySMF directory and jar file:
// SET EZSMFDIR='./java/easysmf-je-1-9-3/jar'
// SET EZSMFJAR='easysmf-je-1.9.3.jar'
//*
//* Location of Java:
// SET JAVA='/usr/lpp/java/J8.0'
//*
//* Compile a Java class using BPXBATCH.
//*
//COMPILE  EXEC PGM=BPXBATCH,REGION=0M
//STDPARM  DD *,SYMBOLS=JCLONLY
SH &JAVA./bin/javac
 -Xlint -verbose
 -cp '&EZSMFDIR/*'
 -d &TGT
 &SRC./&CLASS.
//STDENV   DD *
//STDOUT   DD SYSOUT=*
//STDERR   DD SYSOUT=*

