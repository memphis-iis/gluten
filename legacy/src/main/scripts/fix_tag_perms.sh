#!/bin/bash

if [ "$(id -u)" != "0" ]; then
    echo "You must run this script using sudo"
    exit 1
fi

SRCDIR=/var/tagging
echo Using build dir: $SRCDIR
cd $SRCDIR
if [ $SRCDIR != $(pwd) ]; then
    echo "Could not get to $SRCDIR"
    exit 1
fi


chown -vR tomcat7:tagging * && chmod -vR g+w *
