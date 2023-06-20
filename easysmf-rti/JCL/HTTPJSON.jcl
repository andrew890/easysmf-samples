//ANDREWRJ JOB CLASS=A,
//             MSGCLASS=H,
//             NOTIFY=&SYSUID
//*                                                          Col 72 -> |
//* ***** Edit with CAPS OFF and NUMBER OFF *****
//*
//* Run a Java program using JZOS Batch Launcher
//* Must run under Java 11
//*
// EXPORT SYMLIST=*
//*
//* Class to run with empty JAR and JARDIR, or
//* CLASS='-jar' with JARDIR and JAR values for an executable jar.
//*
// SET CLASS='com.smfreports.sample.RtiHttpJson'
// SET JARDIR=''
// SET JAR=''
//*
//*SET CLASS='-jar'
//*SET JARDIR='./easysmf-je-2.0.6/samples/jar/'
//*SET JAR='easysmf-rti-simple-1.0.0.jar'
//*
//* Java target directory
//* As distributed, relative to user's home directory
//* The target directory will be searched first for
//* classes and dependencies, then target/lib, then the
//* &EZSMFDIR./jar, &EZSMFDIR./samples/jar and
//* &EZSMFDIR./samples/jar/lib directories
//* All CLASSPATH values are ignored for an executable jar.
//*
// SET TGT='./java/rti-http-json'
//*
//* EasySMF directory:
// SET EZSMFDIR='./easysmf-je-2.0.6'
//*
//* Location of JZOS batch launcher module JVMLDM86:
// SET JZOSLIB=VENDOR.LINKLIBE
//*
//* Location of Java:
// SET JAVA='/usr/lpp/java/J11.0_64'
//*
//* SMF data to process
// SET SMFINMEM=IFASMF.ALLRECS
// SET URL='http://107.20.16.13:9999/easysmf'
//*
//* Run a Java program under JZOS Batch Launcher
//*
//G        EXEC PGM=JVMLDM16,REGION=0M,
// PARM='/ &CLASS &JARDIR.&JAR.'
//*
//STEPLIB  DD DISP=SHR,DSN=&JZOSLIB
//*
//MAINARGS DD *,DLM=$$,SYMBOLS=JCLONLY
 &SMFINMEM &URL
$$
//SYSPRINT DD SYSOUT=*
//SYSOUT   DD SYSOUT=*
//STDOUT   DD SYSOUT=*
//STDERR   DD SYSOUT=*
//CEEDUMP  DD SYSOUT=*
//ABNLIGNR DD DUMMY
//*
//* EasySMF Key - get a 30 day trial from
//* https://www.blackhillsoftware.com/30-day-trial/
//* This sample key expires 2023-04-15
//*
//EZSMFKEY DD *
**License:
MQ0KMjAyMy0wNi0yNA0KRXZhbHVhdGlvbg0KSkUsUlRJDQo=
**Sig:
3wjod6unVui/sIGWm7XUg/PxewQ0VY01a9GDO+gPEoH5DTDAUvGUbfEo/V76BTF5
zP/BZluubHEQ90CHXba1EMwg2I8OCMDdpN/OcvrNe+8SPE8DLoPZ5cH2jI/3uaGz
qnIVPAyiE81k0a4m3hBhbZXXaZi38C7SBMam1xqG000=
**End
//*
//* Configure for JZOS: based on JVMJCL16/JVMPRC16
//*
//STDENV   DD *,SYMBOLS=JCLONLY
. /etc/profile
export JAVA_HOME=&JAVA
export PATH=/bin:"${JAVA_HOME}"/bin

LIBPATH=/lib:/usr/lib:"${JAVA_HOME}"/bin
LIBPATH="${LIBPATH}":"${JAVA_HOME}"/lib
LIBPATH="${LIBPATH}":"${JAVA_HOME}"/lib/j9vm
export LIBPATH="${LIBPATH}":

CLASSPATH="${CLASSPATH}":"&TGT."
for i in "&TGT."/*.jar; do
    CLASSPATH="${CLASSPATH}":"${i}"
    done
for i in "&TGT./lib"/*.jar; do
    CLASSPATH="${CLASSPATH}":"${i}"
    done
for i in "&EZSMFDIR./jar"/*.jar; do
    CLASSPATH="${CLASSPATH}":"${i}"
    done
for i in "&EZSMFDIR./samples/jar"/*.jar; do
    CLASSPATH="${CLASSPATH}":"${i}"
    done
for i in "&EZSMFDIR./samples/jar/lib"/*.jar; do
    CLASSPATH="${CLASSPATH}":"${i}"
    done
export CLASSPATH="${CLASSPATH}":

IJO="-Xms512m -Xmx1024m"
export IBM_JAVA_OPTIONS="${IJO} "
