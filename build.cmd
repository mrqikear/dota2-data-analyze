@echo off
setlocal enabledelayedexpansion

:: 1. 检测 Java 路径
if exist "E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20\bin\java.exe" (
    set "JAVA_HOME=E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20"
)

:: 2. 检测 Maven 路径
set "MVN_CMD=mvn"
if exist "E:\maven\apache-maven-3.6.1\bin\mvn.cmd" (
    set "MVN_CMD=E:\maven\apache-maven-3.6.1\bin\mvn.cmd"
)

cd /d %~dp0
call %MVN_CMD% %*
