#!/usr/bin/env python

# TODO: OUR GOOGLE LOGIN IS BROKEN FOR PYTHON 3- switch from our current scheme
#       to Flask-Dance

# TODO: Vagrantfile for testing
# TODO: audit records
# TODO: actually provide prev and next files for edit screen
# TODO: make sure we're checking that edit isn't letting them edit someone
#       else's file
# TODO: actual config for both testing and AWS - should include:
#       ensure_database, FLASK_SECRET, and setups for both test/local and AWS
# TODO: taxnonomy - sort act, subact, modes correctly in edit dialog
# TODO: completed transcripts are VIEW ONLY

import sys
if sys.version_info < (3, 0):
    sys.stderr.write("Sorry, requires Python 3.4 or later\n")
    sys.exit(1)

import os
import logging

from flask import Flask

from gludb.config import Database, default_database

from gluten.utils import project_file
from gluten.models import User, Taxonomy, Transcript
from gluten.auth.google_auth import auth
from gluten.main.app import main

# Note that application as the main WSGI app is required for Python apps
# on Elastic Beanstalk
application = Flask(__name__)
application.secret_key = application.config.get('FLASK_SECRET', "defsecret")

# Final app settings depending on whether or not we are set for debug mode
if os.environ.get('DEBUG', None):
    # Debug mode - running on a workstation
    application.debug = True
    logging.basicConfig(level=logging.DEBUG)
else:
    # We are running on AWS Elastic Beanstalk (or something like it)
    application.debug = False
    logging.basicConfig(level=logging.INFO)

# Register any blueprints we need
application.register_blueprint(auth)
application.register_blueprint(main)


# This will be called before the first request is ever serviced
@application.before_first_request
def before_first():
    # Final app settings depending on whether or not we are set for debug mode
    if application.debug:
        # Debug/local dev
        default_database(Database('sqlite', filename=':memory:'))
    else:
        # Production!
        default_database(Database('dynamodb'))

    # Make sure we have our tables
    User.ensure_table()
    Transcript.ensure_table()
    Taxonomy.ensure_table()

    # Some debug data we might find useful
    if application.debug:
        me = User(name='Test User', email='cnkelly@memphis.edu')
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
    application.run()
if __name__ == '__main__':
    main()
