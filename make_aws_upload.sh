#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

if [ ! -s prod.config ]
then
    echo "You must supply a valid prod.config file!"
    exit 1
fi

# Create the upload archive, skipping our environment and compiled Python files
zip -r ../gluten_aws.zip . -x legacy/\* .git/\* env/\* \*.sh \*.pyc \*.pyo .gitignore __pycache__/\* .vagrant/\* .test.db test.config
