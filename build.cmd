@echo off
if exist "E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20" set JAVA_HOME=E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20
cd /d %~dp0
E:\maven\apache-maven-3.6.1\bin\mvn.cmd %*
