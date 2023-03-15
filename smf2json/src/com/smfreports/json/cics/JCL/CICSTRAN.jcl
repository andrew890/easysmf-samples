//JOBNAME  JOB CLASS=A,
//             MSGCLASS=H,
//             NOTIFY=&SYSUID
//*
//* Run EasySMF:JE using JZOS batch launcher.
//* Based on sample JCL provided with z/OS JAVA JZOS.
//* Original sample JCL is Licensed Materials Property of IBM.
//*                                                            Col 72->|
// EXPORT SYMLIST=*
//*
//* CICS transaction reports require CICS dictionary entries
//* concatenated before the CICS transaction data. See the
//* CICSDICT symbol.
//*
//* Customize symbols as required:
//*
//* Location of sample jar files and dependencies (EasySMF etc):
// SET  APPHOME='/home/<userid>/java/easysmf-samples'
//*
// SET JAVAHOME='/usr/lpp/java/J8.0'
//*
// SET CICSDICT=MY.CICS.DICTIONARY.RECORDS
// SET    INDSN=MY.INPUT.SMF.DATASET
// SET   OUTDSN=MY.OUTPUT.JSON.DATASET
//*
//*
//JAVA     EXEC PROC=JVMPRC80,
//* Uncomment class as desired:
// JAVACLS='com.smfreports.json.cics.CicsAbendTransactions'
//*JAVACLS='com.smfreports.json.cics.CicsTransactionSummary'
//*JAVACLS='com.smfreports.json.cics.CicsTransactionSummaryCustom'
//*JAVACLS='com.smfreports.json.cics.CicsSlowTransactions'
//*
//MAINARGS DD *
--indd  INPUT
--outdd OUTPUT
//* MAINARGS option required for CicsSlowTransactions:
//* --milliseconds 500
//INPUT    DD DISP=SHR,DSN=&CICSDICT
//         DD DISP=SHR,DSN=&INDSN
//OUTPUT   DD DISP=(NEW,CATLG),UNIT=SYSDA,DSN=&OUTDSN,
//            RECFM=VB,LRECL=27994,BLKSIZE=27998,
//            SPACE=(1,(500,200),RLSE),AVGREC=M
//*
//* The following evaluation key is valid until 16 March 2023
//* Request a 30 day evaluation key at:
//* https://www.blackhillsoftware.com/30-day-trial/
//*
//EZSMFKEY DD *
**License:
MQ0KMjAyMy0wMy0xNg0KVGVtcG9yYXJ5IEtleQ0K
**Sig:
FuHoSh3wb6CnZkAkQbgErJnMebKA33g3Ytl8zoKNpZ+ypbcJceVGbCRIfT/uqlR3
n3GlhkMTV4ZaQY4JI2/QbolvCmE7D2ZBFx3d7EYajyaR/dPw0bXkwTEED73i8f7/
n3O4eqja9O0ATxQUheTF0Gjnd/RzKcL5S7dyE6cEpPw=
**End
//*
//STDENV DD *,SYMBOLS=JCLONLY
# This is a shell script which configures
# any environment variables for the Java JVM.
# Variables must be exported to be seen by the launcher.

. /etc/profile
export JAVA_HOME=&JAVAHOME

export PATH=/bin:"${JAVA_HOME}"/bin

LIBPATH=/lib:/usr/lib:"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
export LIBPATH="$LIBPATH":

# Customize your CLASSPATH here
APP_HOME=&APPHOME
CLASSPATH=$APP_HOME:"${JAVA_HOME}"/lib:"${JAVA_HOME}"/lib/ext

# Add Application required jars to end of CLASSPATH
for i in "${APP_HOME}"/*.jar; do
    CLASSPATH="$CLASSPATH":"$i"
    done
export CLASSPATH="$CLASSPATH":

# Set JZOS specific options
# Use this variable to specify encoding for DD STDOUT and STDERR
#export JZOS_OUTPUT_ENCODING=Cp1047
# Use this variable to prevent JZOS from handling MVS operator commands
#export JZOS_ENABLE_MVS_COMMANDS=false
# Use this variable to supply additional arguments to main
#export JZOS_MAIN_ARGS=""

# Configure JVM options
IJO="-Xms512m -Xmx1024m"
# Uncomment the following to aid in debugging "Class Not Found" problems
#IJO="$IJO -verbose:class"
# Uncomment the following if you want to run with Ascii file encoding..
#IJO="$IJO -Dfile.encoding=ISO8859-1"
export IBM_JAVA_OPTIONS="$IJO "

//