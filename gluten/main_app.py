import datetime
import json

from collections import OrderedDict

import flask
from flask import (
    Blueprint,
    abort,
    redirect,
    url_for,
    request,
    flash,
    Response,
    jsonify,
    g
)

from .utils import project_file, template, user_audit_record
from .auth import require_login
from .models import Taxonomy, Transcript

main = Blueprint('main', __name__)


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

    acts = OrderedDict()
    for act in tax.acts:
        act = act['act']  # artifact of our YAML format that we drop for JS
        name = act['name']
        acts[name] = {'name': name, 'subtypes': act['subtypes']}

    return {
        'modes': tax.modes,
        'tagger_supplied': [q['question'] for q in tax.tagger_supplied],
        'acts': acts
    }


def speaker_display(speaker):
    chk = str(speaker).strip().lower()
    if chk.endswith('(customer)'):
        return 'Learner'
    elif chk == 'you':
        return 'Tutor'
    elif chk == 'system message':
        return 'Sys Msg'
    else:
        return speaker


# This will be called before every request, so we can set up any global data
# that we want all requests to see
@main.before_request
def before_request():
    flask.g.year = datetime.datetime.now().year


# Our home/index page (GET only)
@main.route('/')
@main.route('/home')
@require_login
def main_page():
    user = getattr(g, 'user')

    owned, assigned, completed = [], [], []

    for t in Transcript.find_by_index('idx_owned', user.id):
        if t.tagger != user.id:
            owned.append(t)  # Assigned will be in one of the other 2 lists

    for t in Transcript.find_by_index('idx_assigned', user.id):
        if t.state == Transcript.STATES[-1]:
            completed.append(t)
        else:
            assigned.append(t)

    for lst in [owned, assigned, completed]:
        lst.sort(key=Transcript.sort_key)

    return template("home.html", **locals())


def get_current_transcript_info(scriptid):
    """Return the triplet (script, tax, user) for the given scriptid as
    needed for edits/saves"""
    user = getattr(g, 'user', None)
    script = get_script(scriptid)

    tax = None
    if script:
        tax = get_taxonomy(script.taxonomy)

    if script and tax:
        # Make sure transcript has the questions for tagger-supplied info
        for tag_supply in tax['tagger_supplied']:
            qname = tag_supply['name']
            if qname not in script.tagger_supplied_answers:
                script.tagger_supplied_answers[qname] = ''

    return script, tax, user


def get_prev_next(userid, scriptid):
    """Return prev and next transcripts relative to the given transcript for
    the given user"""
    prevFile, nextFile = None, None

    working = [
        t for t in Transcript.find_by_index('idx_assigned', userid)
        if t.state != Transcript.STATES[-1]
    ]

    working.sort(key=Transcript.sort_key)

    for idx, t in enumerate(working):
        if t.id == scriptid:
            if idx > 0:
                prevFile = working[idx-1]
            if idx < len(working) - 1:
                nextFile = working[idx+1]
            break

    return prevFile, nextFile


# Actual annotation page
@main.route('/edit/<scriptid>', methods=['GET', 'POST'])
@require_login
def edit_page(scriptid):
    # Get the transcript and taxonomy
    script, tax, user = get_current_transcript_info(scriptid)
    userid = getattr(user, 'id', None)

    if not script or not tax or not user or not userid:
        return abort(404 if not user else 500)

    # An edit can be in different states
    assessMode = False
    if script.state == Transcript.STATES[-1]:
        assessMode = True  # Transcript is completed
    elif userid not in [script.owner, script.tagger]:
        assessMode = True  # Current user doesn't have write rights

    # We can delegate the rest if we are a POST (a save)
    if request.method == 'POST':
        if assessMode:
            return abort(500)  # They aren't allowed to save in assess mode
        return save_page(script, tax, user)

    # We know that from here down, this is a GET (an edit/display)

    # If the script is still pending, mark it as in progress
    if script.state == Transcript.STATES[0]:
        script.mark_in_progress()
        script.save()
        user_audit_record(script, "Transcript MOVED Pending to InProgress")

    # Add any extra data they might need
    for utt in script.utterance_list:
        utt['disp_speaker'] = speaker_display(utt['speaker'])

    # Figure out previous and next
    prevFile, nextFile = get_prev_next(userid, scriptid)

    # We're finally ready
    user_audit_record(script, "DISPLAY Transcript")
    return template(
        "edit.html",
        transcript=script,
        trainer_transcript=script,
        taxonomy=tax,
        trainingMode=False,
        verifyMode=False,
        assessMode=assessMode,
        prevFile=prevFile,
        nextFile=nextFile
    )


def save_page(script, tax, user):
    # Get the actual save request parameters
    raw_data = request.values.get('fulldata', '')
    if not raw_data:
        # Not sure what this is, but it's nothing we can handle
        flash("No data was found, so nothing was saved")
        user_audit_record(script, "Transcript SAVE-SKIPPED - no fulldata")
        return redirect(url_for('main.edit_page', scriptid=script.id))

    is_autosave = request.values.get('autosave', False)
    is_complete = request.values.get('completed', False)
    autosave_err = ''

    # Get the tagger-supplied answers we expect for the current taxonomy
    for tag_supply in tax['tagger_supplied']:
        qname = tag_supply['name']
        qanswer = request.values.get(qname, '')
        script.tagger_supplied_answers[qname] = qanswer

    # Save the utterance list
    saved_data = json.loads(raw_data)

    for src_utt in saved_data:
        index = int(src_utt.get("index"))

        conf = src_utt['confidence']
        conf = int(conf) if conf else 1

        dest = script.utterance_list[index]
        dest['act'] = src_utt['act']
        dest['subact'] = src_utt['subact']
        dest['mode'] = src_utt['mode']
        dest['comments'] = src_utt['comments']
        dest['tag_confidence'] = conf

    script.save()

    if is_autosave:
        # On autosave, there is no redirect - they get back a JSON response
        user_audit_record(script, "Transcript AUTOSAVE Accepted")
        return jsonify({
            'success': True if not autosave_err else False,
            'errmsg': autosave_err
        })
    elif is_complete:
        # Mark complete and then redirect back home
        if script.state != Transcript.STATES[-1]:
            script.mark_completed()
            script.save()
            user_audit_record(script, "Transcript COMPLETE-BTN Accepted")
        flash("The transcript was marked completed")
        return redirect(url_for('main.main_page'))
    else:
        # Normal save - they get to keep editing
        flash("Your changes were saved")
        user_audit_record(script, "Transcript SAVE-BTN Accepted")
        return redirect(url_for('main.edit_page', scriptid=script.id))


# Return the taxonomy (in JSON) for the given transcript - note that we
# explicitly create the dict that we JSONify. This keeps private data private,
# but it also let's us remove some of the weirdness that comes from our YAML
# format
@main.route('/taxonomy/<scriptid>', methods=['GET'])
@require_login
def taxonomy_page(scriptid):
    script = get_script(scriptid)
    taxid = script.taxonomy if script and script.taxonomy else ''

    # Note that we want to be able to include <script> tag with a taxonomy URL
    # that will magically create a JS object named taxonomy. As a result, we
    # can't quite use the magic of Flask jsonify
    data = "taxonomy = " + json.dumps(get_taxonomy(taxid)) + ";"
    return Response(data, mimetype='application/json')
