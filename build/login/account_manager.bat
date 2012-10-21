@echo off
title Account Manager

REM ########################################################################
REM # You need to set here your JDK/JRE params in case of x64 bits System. #
REM # Remove the "REM" after set PATH variable                             #
REM # If you're not a x64 system user just leave                           # 
REM ########################################################################
REM set PATH="type here your path to java jdk/jre (including bin folder)"

@java -Xbootclasspath/p:libs/crypt.jar -cp ../libs/*;./login.jar  -Djava.util.logging.config.file=console.cfg ru.catssoftware.accountmanager.AccountManager

