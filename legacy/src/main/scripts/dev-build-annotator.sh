#!/bin/bash

SRCDIR=$HOME/annotator

echo Using build dir: $SRCDIR
cd $SRCDIR
if [ $SRCDIR != $(pwd) ]; then
    echo "Could not get to $SRCDIR - have you checked it out from SVN?"
    exit 1
fi


svn update
mvn clean install site:site site:deploy
