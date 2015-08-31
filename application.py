#!/usr/bin/env python

import sys
if sys.version_info < (3, 0):
    sys.stderr.write("Sorry, requires Python 3.4 or later\n")
    sys.exit(1)

import os
import os.path as pth
import logging
import datetime

import flask
from flask import (
    Flask,
    render_template,
    abort,
    redirect,
    url_for,
    request,
    jsonify
)

from gluten.models import User, Taxonomy, Transcript

from gludb.config import Database, default_database

# TODO: Vagrantfile for testing
# TODO: google (social) auth/login
# TODO: model and storage
# TODO: audit records
# TODO: make sure we're checking that edit isn't letting them edit someone
#       else's file

# TODO: actual config for both testing and AWS - should include:
#       ensure_database, FLASK_SECRET, and setups for both test/local and AWS

# Note that application as the main WSGI app is required for Python apps
# on Elastic Beanstalk
application = Flask(__name__)
application.secret_key = application.config.get('FLASK_SECRET', "defsecret")


def project_file(relpath):
    """Given the path to a file relative to the project root, return the
    absolute file name. We depend on the fact that this file is IN the project
    root"""
    base = pth.abspath(pth.dirname(__file__))
    return pth.join(base, relpath)


def get_script(scriptid):
    return Transcript.find_one(scriptid) if scriptid else None


# This will be called before the first request is ever serviced
@application.before_first_request
def before_first():
    # Final app settings depending on whether or not we are set for debug mode
    if os.environ.get('DEBUG', None):
        default_database(Database('sqlite', filename=':memory:'))
        Transcript.ensure_table()
        Transcript.from_xml_file(
            project_file('test/sample/SampleTranscript.xml')
        ).save()
    else:
        default_database(Database('dynamodb'))

    User.ensure_table()
    Taxonomy.ensure_table()
    Transcript.ensure_table()


# This will be called before every request, so we can set up any global data
# that we want all requests to see
@application.before_request
def before_request():
    flask.g.year = datetime.datetime.now().year


# Our home/index page (GET only)
@application.route('/')
@application.route('/home')
def main_page():
    return render_template("home.html")  # TODO


# Assign your transcripts (with taxonomy) to other people
@application.route('/admin-assign', methods=['GET', 'POST'])
def admin_assign():
    return render_template("home.html")  # TODO


# Actual annotation page
@application.route('/edit/<scriptid>', methods=['GET', 'POST'])
def edit_page(scriptid):
    script = get_script(scriptid)
    if not script:
        abort(404)

    if request.method == 'GET':
        return render_template("edit.html", transcript=script)

    # From here on down we're in POST

    # TODO: handle auto-save
    # TODO: handle "real" save

    return redirect(url_for('edit_page', scriptid=scriptid))


# Return the taxonomy (in JSON) for the given transcript - note that we
# explicitly create the dict that we JSONify. This keeps private data private,
# but it also let's us remove some of the weirdness that comes from our YAML
# format
@application.route('/taxonomy/<scriptid>', methods=['GET'])
def taxonomy_page(scriptid):
    taxid = None

    script = get_script(scriptid)
    taxid = script.taxonomy or ''
    tax = Taxonomy.find_one(taxid) if taxid else None

    if not tax:
        tax = Taxonomy.from_yaml_file(
            project_file('config/default_taxonomy.yaml')
        )

    acts = {}
    for act in tax.acts:
        act = act['act']  # artifact of our YAML format that we drop for JS
        name = act['name']
        acts[name] = {'name': name, 'subtypes': act['subtypes']}

    return jsonify(taxonomy={
        'modes': tax.modes,
        'tagger_supplied': [q['question'] for q in tax.tagger_supplied],
        'acts': acts
    })


# Final app settings depending on whether or not we are set for debug mode
if os.environ.get('DEBUG', None):
    # Debug mode - running on a workstation
    application.debug = True
    logging.basicConfig(level=logging.DEBUG)
else:
    # We are running on AWS Elastic Beanstalk (or something like it)
    application.debug = False
    logging.basicConfig(level=logging.INFO)


# Our entry point - called when our application is started "locally".
# This WILL NOT be run by Elastic Beanstalk
def main():
    application.run()
if __name__ == '__main__':
    main()
