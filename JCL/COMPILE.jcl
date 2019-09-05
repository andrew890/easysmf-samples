//JOBNAME  JOB CLASS=A,                                          
//             MSGCLASS=H,                                       
//             NOTIFY=&SYSUID                                    
//*                                                              
//* Compile a Java class using BPXBATCH.                         
//* java/src/RecordCount.java is the file to compile.            
//* Output goes to java/target                                   
//*                                                              
//* The EasySMF jar files have been installed in                 
//* java/easysmf-je-1-9-1/jar                                    
//*                                                              
//* The directories are relative to the user's home directory    
//* because java/target etc are relative paths (no leading "/")  
//*                                                              
//* RecordCount.java specifies a package of com.smfreports, which
//* means that the compiled class file goes to the com/smfreports
//* subdirectory under java/target i.e. if the home directory is 
//* /u/userid, the output will be                                
//* /u/userid/java/target/com/smfreports/RecordCount.class       
//*                                                              
//* Java uses the same convention when running the program.      
//* If CLASSPATH points to java/target and the class is          
//* com.smfreports.RecordCount, Java will look for               
//* java/target/com/smfreports/RecordCount.class                 
//*                                                              
//COMPILE  EXEC PGM=BPXBATCH,REGION=0M                           
//STDPARM  DD *                                                  
SH /usr/lpp/java/J8.0/bin/javac                                  
 -Xlint -verbose                                                 
 -cp 'java/easysmf-je-1-9-1/jar/*'                               
 -d java/target                                                  
 java/src/RecordCount.java                                       
//STDENV   DD *                                                  
//STDOUT   DD SYSOUT=*                                           
//STDERR   DD SYSOUT=*                                           