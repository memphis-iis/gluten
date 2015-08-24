#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

# Create the upload archive, skipping our environment and compiled Python files
zip -r ../gluten.zip . -x .git/\* env/\* \*.sh \*.pyc \*.pyo .gitignore
