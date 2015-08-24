#!/bin/bash

# Make sure we're in our script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

# Set up a virtual environment using python3, use the new environment, and
# install our dependencies
virtualenv -p python3 env
source $SCRIPT_DIR/env/bin/activate
pip install -r requirements.txt
