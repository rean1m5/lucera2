#!/bin/sh

if [ -f mysql_settings.sh ]; then
        . mysql_settings.sh
else
        echo "Can't find mysql_settings.sh file!"
        exit
fi

for sqlfile in login/*.sql
do
        echo Loading $sqlfile ...
        mysql -h $LOGIN_DBHOST -u $LOGIN_USER --password=$LOGIN_PASS -D $LOGIN_DBNAME < $sqlfile
done