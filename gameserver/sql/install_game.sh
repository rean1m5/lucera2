#!/bin/sh

if [ -f mysql_settings.sh ]; then
        . mysql_settings.sh
else
        echo "Can't find mysql_settings.sh file!"
        exit
fi

for sqlfile in server/*.sql
do
        echo Loading $sqlfile ...
        mysql -h $GAME_DBHOST -u $GAME_USER --password=$GAME_PASS -D $GAME_DBNAME < $sqlfile
done