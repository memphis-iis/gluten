#!/bin/bash

if [ "$(id -u)" != "0" ]; then
    echo "You must run this script using sudo"
    exit 1
fi

SRCDIR=$HOME/assign
STUDENTS=$SRCDIR/students.txt
ASSIGNMENTS=$SRCDIR/to_assign.txt

BASEDIR=/var/tagging/userdata
TRAINSRC=$BASEDIR/untagged/Pending


################################################################
# Verify everything ready to go
check_file() {
    echo "Using $1 file: $2"
    if [ ! -f $2 ]; then
        echo "Could not find $1 file: $2"
        exit 1
    fi
}
check_dir() {
    echo "Using $1 dir: $2"
    cd $2
    if [ $2 != $(pwd) ]; then
        echo "Could not get to $1 dir $2"
        exit 1
    fi
}
do_verify() {
    check_file student $STUDENTS
    check_file assignment $ASSIGNMENTS
    check_dir base $BASEDIR
    check_dir training $TRAINSRC
}

################################################################
# Take given student id (email) and assign all files
do_one() {
    TARGET=$BASEDIR/$1/Pending
    sudo mkdir -p $TARGET
    while read p; do
        echo "Assigning $p to $1"
        sudo cp $TRAINSRC/$p $TARGET
    done < $ASSIGNMENTS
}

################################################################
# main logic

#Verify parms and dole out assignments
do_verify
while read student; do
    do_one $student
done < $STUDENTS

#Update all permissions
echo "Updating all permissions in user data"
sudo chown -R tomcat7:tagging $BASEDIR/*
sudo chmod -R g+w $BASEDIR/*
