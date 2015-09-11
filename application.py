#!/usr/bin/env python

# TODO: define an order for transcripts that we use on all home views

# TODO: completed transcripts shouldn't be in Assigned view - they should be in
#       a new Completed View

# TODO: actually provide prev and next files for edit screen

# TODO: audit records

# TODO: deploy script that builds a zip file for AWS *and* enforces a
#       prod.config file *and* warns you to remember to set that env var in
#       the AWS EB settings

import sys
if sys.version_info < (3, 0):
    sys.stderr.write("Sorry, requires Python 3.4 or later\n")
    sys.exit(1)

import os
import logging

from flask import Flask

from gludb.config import Database, default_database

from config import env_populate
from gluten.utils import project_file
from gluten.models import User, Taxonomy, Transcript
from gluten.auth import auth
from gluten.main_app import main
from gluten.admin import admin


# Note that application as the main WSGI app is required for Python apps
# on Elastic Beanstalk. Also note that we provide the default config, but
# someone must supply an actual config file pointed to by the env variable
# GLUTEN_CONFIG_FILE. See ./local.sh for an example of how to handle this
application = Flask(__name__)
application.config.from_object('config.DefaultConfig')
application.config.from_envvar('GLUTEN_CONFIG_FILE')
application.secret_key = application.config.get('FLASK_SECRET')

# Set any environment var's requested by the config file
for name in env_populate:
    os.environ[name] = application.config.get(name)

# Final app settings depending on whether or not we are set for debug mode
if application.config.get('DEBUG', None):
    # Debug mode - running on a workstation
    application.debug = True
    logging.basicConfig(level=logging.DEBUG)
else:
    # We are running on AWS Elastic Beanstalk (or something like it)
    application.debug = False
    logging.basicConfig(level=logging.INFO)
logging.getLogger('gluten').info('Application debug is %s', application.debug)

# Register our blueprints
application.register_blueprint(auth)
application.register_blueprint(main)
application.register_blueprint(admin)


# This will be called before the first request is ever serviced
@application.before_first_request
def before_first():
    if application.debug:
        # Debug/local dev
        default_database(Database('sqlite', filename=project_file('.test.db')))
    else:
        # Production!
        default_database(Database('dynamodb'))

    # Make sure we have our tables
    User.ensure_table()
    Transcript.ensure_table()
    Taxonomy.ensure_table()

    # Some debug data we might find useful
    if application.debug:
        TEST_EMAIL = application.config.get('TEST_EMAIL')
        me = User.find_by_index('idx_email', TEST_EMAIL)
        if not me:
            me = User(name='Test User', email=TEST_EMAIL)
            me.save()

        ts1 = Transcript.from_xml_file(
            project_file('test/sample/SampleTranscript.xml')
        )
        ts1.script_identifier = 'Original Owned'
        ts1.owner = me.id
        ts1.tagger = ''
        ts1.id = ''
        ts1.save()

        ts2 = Transcript.from_xml_file(
            project_file('test/sample/SampleTranscript.xml')
        )
        ts2.script_identifier = 'New Assigned'
        ts2.owner = me.id
        ts2.tagger = me.id
        ts2.source_transcript = ts1.id
        ts2.id = ''  # Ensure new id on save
        ts2.save()


# Our entry point - called when our application is started "locally".
# This WILL NOT be run by Elastic Beanstalk
def main():
    # Listen on all addresses if running under Vagrant, else listen
    # on localhost
    host = '0.0.0.0' if os.environ['USER'] == 'vagrant' else '127.0.0.1'
    application.run(host=host)
if __name__ == '__main__':
    main()
