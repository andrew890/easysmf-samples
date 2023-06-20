//JOBNAME  JOB CLASS=A,
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
//* Class to run:
//*
// SET CLASS='com.smfreports.sample.RtiHttpBinary'
//*
//* Java target directory.
//* As distributed, relative to user's home directory.
//* The target directory will be searched first for
//* classes and dependencies, then target/lib.
//*
// SET TGT='./java/rti-http-binary'
//*
//* Location of JZOS batch launcher module JVMLDM16:
// SET JZOSLIB=JZOS.LINKLIBE
//*
//* Location of Java:
// SET JAVA='/usr/lpp/java/J11.0_64'
//*
//* SMF data to process
// SET SMFINMEM=IFASMF.MYRECS
// SET URL='http://192.168.12.34:9999/easysmf'
//*
//* Run a Java program under JZOS Batch Launcher
//*
//G        EXEC PGM=JVMLDM16,REGION=0M,
// PARM='/ &CLASS'
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
export CLASSPATH="${CLASSPATH}":

IJO="-Xms512m -Xmx1024m"
export IBM_JAVA_OPTIONS="${IJO} "
