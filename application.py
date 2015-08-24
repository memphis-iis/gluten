#!/usr/bin/env python

import os
import logging
import datetime

import flask
from flask import Flask, render_template

# Note that application as the main WSGI app is required for Python apps
# on Elastic Beanstalk
application = Flask(__name__)
# TODO: actual config for both testing and AWS
application.secret_key = application.config.get('FLASK_SECRET', "defsecret")


def ensure_tables():
    pass  # TODO: this should be in our config stuff


# This will be called before the first request is ever serviced
@application.before_first_request
def before_first():
    ensure_tables()


# This will be called before every request, so we can set up any global data
# that we want all requests to see
@application.before_request
def before_request():
    flask.g.year = datetime.datetime.now().year


# Our home/index page
@application.route('/')
@application.route('/home')
def main_page():
    return render_template("home.html")


# Final app settings depending on whether or not we are set for debug mode
if os.environ.get('DEBUG', None):
    # Debug mode - running on a workstation
    application.debug = True
    logging.basicConfig(level=logging.DEBUG)
else:
    # We are running on AWS Elastic Beanstalk (or something like it)
    application.debug = False
    logging.basicConfig(level=logging.INFO)


# Our entry point - called when our application is started "locally". NOTE that
# this WILL NOT be run by Elastic Beanstalk
def main():
    application.run()
if __name__ == '__main__':
    main()
