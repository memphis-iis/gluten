#!/bin/bash

#Note that we assume that we are running from the directory that where
#everything is located.  If you want to move this script, set the $SCRIPT_DIR
#variable some other way

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source $SCRIPT_DIR/vars.sh

cd $SCRIPT_DIR
source $GLUTEN_VENV/env/bin/activate
which python

#Run tests! (with any extra parameters passed in)
python $(which nosetests) $*
