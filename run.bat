@echo off
rem Compiles and starts ByteForge. Needs JDK 11+ (javac on PATH or JAVA_HOME set).
rem   run.bat                       -> GUI
rem   run.bat --console             -> console demo of all patterns
rem   run.bat --test                -> automated tests
rem   run.bat --screenshots <dir>   -> save GUI screenshots
setlocal
cd /d "%~dp0"
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME (
  set "JAVAC=%JAVA_HOME%\bin\javac"
  set "JAVA=%JAVA_HOME%\bin\java"
)
if exist out rmdir /s /q out
dir /s /b src\*.java test\*.java > sources.txt
"%JAVAC%" -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  echo Compilation failed. Make sure a JDK 11+ is installed.
  exit /b 1
)
del sources.txt
if "%~1"=="--test" (
  "%JAVA%" -cp out com.byteforge.PatternTests
) else (
  "%JAVA%" -cp out com.byteforge.Main %*
)
