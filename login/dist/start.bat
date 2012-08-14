@echo off
title Login Server Console
:start
java -Xbootclasspath/p:libs/crypt.jar -Dfile.encoding=UTF-8 -Xmx128m -cp ./login.jar;../libs/* ru.catssoftware.loginserver.L2LoginServer

if ERRORLEVEL 2 goto start
