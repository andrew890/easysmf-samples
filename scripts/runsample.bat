@REM     Instructions
@REM
@REM     1) Change EASYSMFLOCATION to the path where EasySMF was installed.
@REM     2) Change EASYSMFKEY to the path of the file with the temporary
@REM        or permanent key.
@REM        Get a 30 day trial key from: 
@REM        https://www.blackhillsoftware.com/30-day-trial/
@REM     3) Run the batch file passing the sample and SMF data file as arguments e.g.:
@REM        runsample com.blackhillsoftware.samples.RecordCount SMF.DATA
@REM
@REM     This sample assumes that required Java environment variables e.g. JAVA_HOME 
@REM     were set by the Java installation process.
@REM

set "EASYSMFLOCATION=C:\path to\easysmf-je-ZVERSION"
set "EASYSMFKEY=C:\path to your\key.txt"

java -classpath "%EASYSMFLOCATION%\samples\*;%EASYSMFLOCATION%\jar\*" %1 %2
