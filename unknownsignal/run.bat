@echo off
rem UNKNOWN SIGNAL — compile and launch (Windows cmd)
cd /d %~dp0
if not exist out mkdir out
dir /s /b server\src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
java -cp out com.freebuff.signal.Main %*