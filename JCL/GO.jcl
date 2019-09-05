//JOBNAME  JOB CLASS=A,                                         
//             MSGCLASS=H,                                      
//             NOTIFY=&SYSUID                                   
//*                                                             
//* Run JZOS batch launcher, specifying the class to run,       
//* in this case class RecordCount in package com.smfreports.   
//* See below for a description of where Java will look for     
//* the file containing the class.                              
//*                                                             
//G        EXEC PGM=JVMLDM80,REGION=0M,                         
//   PARM='/ com.smfreports.RecordCount'                        
//*                                                             
//* Location of JZOS batch launcher module JVMLDM80:            
//STEPLIB  DD DISP=SHR,DSN=JZOS.LINKLIBE                        
//*                                                             
//INPUT    DD DISP=SHR,DSN=MVS1.SMF.RECORDS                     
//SYSPRINT DD SYSOUT=*                                          
//SYSOUT   DD SYSOUT=*                                          
//STDOUT   DD SYSOUT=*                                          
//STDERR   DD SYSOUT=*                                          
//CEEDUMP  DD SYSOUT=*                                          
//ABNLIGNR DD DUMMY                                             
//*                                                             
//* EasySMF Key - get a 30 day trial from                       
//* https://www.blackhillsoftware.com/30-day-trial/             
//EZSMFKEY DD *                                                 
**License:                                                      
MQ0KMjAxOS0wNC0wMw0KVGVtcG9yYXJ5IEtleQ0K                        
**Sig:                                                          
thslgMuv3PV9Y5pDzRoU+2ix0g+PSucQqNNxh6+5ye+31oh3yg+W02t8eSo6msxB
ON35Wu7Mk1rw4kdZ3pL+RMj1LQ43oXIK5pyojznP4BS3ct6zD6vbLY5TH82lNSxQ
ox3bEAQwDEbZtgdvprI30hUk8PMOUTyCWXwyQ10sL+I=                    
**End                                                           
//*                                                             
//* Set up the environment for JZOS batch launcher.             
//* java/target is the base for searching for the class to      
//* run (relative to the home directory).                       
//* If the home directory is /u/userid and the class is         
//* com.smfreports.RecordCount then Java will attempt           
//* to launch class file                                        
//* /u/userid/java/target/com/smfreports/RecordCount.class      
//*                                                             
//STDENV   DD *                                               
. /etc/profile                                                
export JAVA_HOME=/usr/lpp/java/J8.0                           
export PATH=/bin:"${JAVA_HOME}"/bin                           
                                                              
LIBPATH=/lib:/usr/lib:"${JAVA_HOME}"/bin                      
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390                    
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm               
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic                 
export LIBPATH="$LIBPATH":                                    
                                                              
APP_HOME=java/target                                          
CP="${APP_HOME}"                                              
CP="${CP}":"java/easysmf-je-1-9-1/jar/easysmf-je-1.9.1.jar"   
CP="${CP}":"java/easysmf-je-1-9-1/jar/slf4j-api-1.7.21.jar"   
CP="${CP}":"java/easysmf-je-1-9-1/jar/slf4j-simple-1.7.21.jar"
export CLASSPATH="${CP}"                                      
                                                              
IJO="-Xms16m -Xmx128m"                                        
export IBM_JAVA_OPTIONS="$IJO "                               