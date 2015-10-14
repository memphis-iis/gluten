#!/bin/bash

# Make sure we're in our script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

source $SCRIPT_DIR/vars.sh

# We remove the previous environment. We should probably just refresh it
# and require a command line flag to blow it away. Future maintainers,
# there's a good task for you :)
rm -fr $GLUTEN_VENV/env/

# Set up a virtual environment using python3, use the new environment, and
# install our dependencies
virtualenv -p python3 $GLUTEN_VENV/env
source $GLUTEN_VENV/env/bin/activate
pip install --upgrade requests[security]
pip install -r requirements.txt
