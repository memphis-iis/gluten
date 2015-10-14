#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

source $SCRIPT_DIR/vars.sh

# Use our virtual environment and run the application in DEBUG mode with our
# test config file. Note that the test config file IS NOT in version control
source $GLUTEN_VENV/env/bin/activate
export GLUTEN_CONFIG_FILE=$SCRIPT_DIR/test.config
python $SCRIPT_DIR/application.py
