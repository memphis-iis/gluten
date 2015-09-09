from flask import (
    Blueprint,
    g,
    request,
    url_for,
    Response,
    redirect,
    flash
)

from .utils import project_file, template
from .auth import require_login
from .models import User, Transcript, Taxonomy

admin = Blueprint('admin', __name__)


@admin.route('/admin', methods=['GET', 'POST'])
@require_login
def admin_page():
    user = getattr(g, 'user')

    # GET is easy...
    if request.method == 'GET':
        return template(
            "admin.html",
            transcripts=Transcript.find_by_index('idx_owned', user.id),
            users=User.find_all(),
            taxonomies=Taxonomy.find_by_index('idx_owned', user.id),
        )

    # POST: They are requesting a transcript assignment

    assignee = request.values.get('user', '')
    script_id = request.values.get('script', '')
    transcript = Transcript.find_one(script_id) if script_id else None
    taxonomy = request.values.get('taxonomy', '')

    if transcript and assignee:
        transcript = transcript.assigned_copy(assignee, taxonomy)
        transcript.save()
        flash("Transcript has been assigned")
    else:
        flash("Nothing assigned")

    return redirect(url_for('admin.admin_page'))


@admin.route('/upload/transcript', methods=['POST'])
@require_login
def upload_transcript():
    user = getattr(g, 'user')
    file = request.files['file']

    if file:
        data = file.read()
        try:
            transcript = Transcript.from_xml(data)
            transcript.owner = user.id
            transcript.save()
            flash("Transcript was saved")
        except:
            flash("Transcript was invalid - nothing was saved", "error")
    else:
        flash("Transcript was NOT uploaded - no file supplied", "error")

    return redirect(url_for('admin.admin_page'))


@admin.route('/upload/taxonomy', methods=['POST'])
@require_login
def upload_taxonomy():
    user = getattr(g, 'user')
    file = request.files['file']
    name = request.values.get('name', '')

    if file and name:
        data = file.read()
        try:
            tax = Taxonomy.from_yaml(data)
            tax.validate()
            tax.name = name
            tax.owner = user.id
            tax.save()
            flash("Taxonomy was saved")
        except:
            flash("Taxonomy was NOT saved - file was invalid", "error")
    else:
        flash("Taxonomy was NOT saved - no file supplied", "error")

    return redirect(url_for('admin.admin_page'))


@admin.route('/sample/transcript', methods=['GET'])
@require_login
def sample_transcript():
    with open(project_file("config/sample_transcript.xml")) as f:
        data = f.read()
    return Response(data, mimetype='text/xml')


@admin.route('/sample/taxonomy', methods=['GET'])
@require_login
def sample_taxonomy():
    with open(project_file("config/default_taxonomy.yaml")) as f:
        data = f.read()
    return Response(data, mimetype='text/x-yaml')
