import datetime
import json

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

from gluten.utils import project_file, require_login, template
from gluten.models import Taxonomy, Transcript

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
    return template(
        "home.html",
        owned=Transcript.find_by_index('idx_owned', user.id),
        assigned=Transcript.find_by_index('idx_assigned', user.id),
    )


# Assign your transcripts (with taxonomy) to other people
@main.route('/admin-assign', methods=['GET', 'POST'])
@require_login
def admin_assign_page():
    return template("home.html")  # TODO: actual assignment screen


# Actual annotation page
@main.route('/edit/<scriptid>', methods=['GET', 'POST'])
@require_login
def edit_page(scriptid):
    script = get_script(scriptid)
    if not script:
        abort(404)

    tax = get_taxonomy(script.taxonomy)
    if not tax:
        return abort(500)  # We are broken

    if request.method == 'GET':
        # If the script is still pending, mark it as in progress
        if script.state == Transcript.STATES[0]:
            script.mark_in_progress()
            script.save()

        # Add any extra data they might need
        for utt in script.utterance_list:
            utt['disp_speaker'] = speaker_display(utt['speaker'])

        return template(
            "edit.html",
            transcript=script,
            taxonomy=tax
        )
    else:
        # Was a POST - do our save
        return save_page(script, tax)


def save_page(script, tax):
    # Get the actual save request parameters
    raw_data = request.values.get('fulldata', '')
    if not raw_data:
        # Not sure what this is, but it's nothing we can handle
        flash("No data was found, so nothing was saved")
        return redirect(url_for('main.edit_page', scriptid=script.id))

    is_autosave = request.values.get('autosave', False)
    is_complete = request.values.get('completed', False)
    autosave_err = ''

    # TODO: look for tagger-supplied top-level values (that are specified in
    #       the taxonomy)

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
        dest['comment'] = src_utt['comments']
        dest['tag_confidence'] = conf

    script.save()

    if is_autosave:
        # On autosave, there is no redirect - they get back a JSON response
        return jsonify({
            'success': True if not autosave_err else False,
            'errmsg': autosave_err
        })
    elif is_complete:
        # Mark complete and then redirect back home
        if script.state != Transcript.STATES[-1]:
            script.mark_completed()
            script.save()
        flash("The transcript was marked completed")
        return redirect(url_for('main.main_page'))
    else:
        # Normal save - they get to keep editing
        flash("Your changes were saved")
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
