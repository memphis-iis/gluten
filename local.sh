#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

# Use our virtual environment and run the application in DEBUG mode
source $SCRIPT_DIR/env/bin/activate
export DEBUG=1
python $SCRIPT_DIR/application.py
