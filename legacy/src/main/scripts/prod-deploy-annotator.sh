#!/bin/bash

if [ "$(id -u)" = "0" ]; then
    echo "You must NOT run this script as root - it uses sudo"
    exit 1
fi

if [ "$(sudo id -u)" != "0" ]; then
    echo "You must be able to run sudo to run this script"
    exit 1
fi


DEPDIR=/var/lib/tomcat7/webapps
DEPFILE=$DEPDIR/annotator.war
SRCFILE=$HOME/annotator.war

echo Using Tomcat7 deploy directory: $DEPDIR
cd $DEPDIR
if [ $DEPDIR != $(pwd) ]; then
    echo "Could not get to $DEPDIR"
    exit 2
fi

echo Using WAR file: $SRCFILE
if [ ! -f $SRCFILE ]; then
    echo "$SRCFILE does not exist - did you forget to copy the file from the build server?"
    exit 3
fi

sudo service tomcat7 stop
sudo rm -f $DEPFILE
sudo rm -fr $DEPDIR/annotator

sudo cp $SRCFILE $DEPFILE
if [ ! -f $DEPFILE ]; then
    echo "COULD NOT COPY $DEPFILE !!!"
    echo "***** IMPORTANT *****"
    echo "Tomcat7 will be restarted, but Annotator won't be running!"
    echo "You need to fix stuff and restart Tomcat7"
fi

sudo service tomcat7 start

