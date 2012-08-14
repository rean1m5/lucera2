#!/bin/sh

java -Xbootclasspath/p:libs/crypt.jar -Djava.util.logging.config.file=console.cfg -cp ../libs/*:./login.jar ru.catssoftware.accountmanager.AccountManager