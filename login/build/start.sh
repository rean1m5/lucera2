#!/bin/sh
err=1
until [ $err == 0 ];
do
	java -Dfile.encoding=UTF-8 -cp ./lib/*:../libs/* -Xmx128m ru.catssoftware.loginserver.L2LoginServer
	err=$?
	sleep 10;
done