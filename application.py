#!/usr/bin/env python

import sys
if sys.version_info < (3, 0):
    sys.stderr.write("Sorry, requires Python 3.4 or later\n")
    sys.exit(1)

import os
import os.path as pth
import logging
import datetime
import json

import flask
from flask import (
    Flask,
    render_template,
    abort,
    redirect,
    url_for,
    request,
    flash,
    Response
)

from gluten.models import User, Taxonomy, Transcript

from gludb.config import Database, default_database

# TODO: Vagrantfile for testing
# TODO: google (social) auth/login
# TODO: audit records
# TODO: actually provide prev and next files for edit screen
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
    """Find and return the matching Transcript (or None)"""
    return Transcript.find_one(scriptid) if scriptid else None


def get_taxonomy(taxid):
    """Find and return the proper taxonomy (which might be the default),
    properly transformed for our templates and client-side JSON"""
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

    return {
        'modes': tax.modes,
        'tagger_supplied': [q['question'] for q in tax.tagger_supplied],
        'acts': acts
    }


def get_user():
    """Return current user"""
    # TODO: actual work
    return User.find_by_index('idx_email', 'cnkelly@memphis.edu')[0]


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


# This will be called before every request, so we can set up any global data
# that we want all requests to see
@application.before_request
def before_request():
    flask.g.year = datetime.datetime.now().year


# Helper that provides any default, base data for our templates
def template(template_name, **context_kwrds):
    ctx = {
        'user': get_user(),
        'is_verifier': False,
        'is_assigner': False,
        'is_assessor': False
    }
    ctx.update(context_kwrds)
    return render_template(template_name, **ctx)


# Our home/index page (GET only)
@application.route('/')
@application.route('/home')
def main_page():
    # TODO: check for qs parm do_logout=yes
    return template(
        "home.html",
        owned=Transcript.find_by_index('idx_owned', get_user().id),
        assigned=Transcript.find_by_index('idx_assigned', get_user().id),
    )


# Assign your transcripts (with taxonomy) to other people
@application.route('/admin-assign', methods=['GET', 'POST'])
def admin_assign_page():
    return template("home.html")  # TODO: actual assignment screen


# Actual annotation page
@application.route('/edit/<scriptid>', methods=['GET', 'POST'])
def edit_page(scriptid):
    script = get_script(scriptid)
    if not script:
        abort(404)

    if request.method == 'GET':
        return template(
            "edit.html",
            transcript=script,
            taxonomy=get_taxonomy(script.taxonomy)
        )

    # From here on down we're in POST

    # TODO: handle auto-save
    # TODO: handle "real" save

    flash("Your changes were saved")
    return redirect(url_for('edit_page', scriptid=scriptid))


# Return the taxonomy (in JSON) for the given transcript - note that we
# explicitly create the dict that we JSONify. This keeps private data private,
# but it also let's us remove some of the weirdness that comes from our YAML
# format
@application.route('/taxonomy/<scriptid>', methods=['GET'])
def taxonomy_page(scriptid):
    script = get_script(scriptid)
    taxid = script.taxonomy if script and script.taxonomy else ''

    # Note that we want to be able to include <script> tag with a taxonomy URL
    # that will magically create a JS object named taxonomy. As a result, we
    # can't quite use the magic of Flask jsonify
    data = "taxonomy = " + json.dumps(get_taxonomy(taxid)) + ";"
    return Response(data, mimetype='application/json')


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
