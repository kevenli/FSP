setlocal
setLocal EnableDelayedExpansion
set CFGDIR=%~dp0%..\conf
set LOG_DIR=%~dp0%..\log
set LOG4J_PROP=INFO,CONSOLE

REM for sanity sake assume Java 1.6
REM see: http://java.sun.com/javase/6/docs/technotes/tools/windows/java.html

REM make it work in the release

set CLASSPATH=
 for /R ../lib %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
set CLASSPATH=!CLASSPATH!

SET CLASSPATH=%~dp0..\lib\*;%CLASSPATH%
SET CLASSPATH=%~dp0..\build;%CLASSPATH%
SET CLASSPATH=%~dp0..\build\classes;%CLASSPATH%
SET CLASSPATH=%~dp0..\build\fspserver-0.1.0.jar;%CLASSPATH%

REM make it work for developers
REM SET CLASSPATH=%~dp0..\build\classes;%CLASSPATH%

set CFG=%CFGDIR%\fsp.cfg

set MAIN=flowy.scheduler.server.FSPServer
echo on
java -cp "%CLASSPATH%" %MAIN% "%CFG%" %*

endlocal