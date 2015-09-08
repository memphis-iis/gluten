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
from .models import Transcript

admin = Blueprint('admin', __name__)


@admin.route('/admin', methods=['GET', 'POST'])
@require_login
def admin_page():
    user = getattr(g, 'user')

    # GET is easy...
    if request.method == 'GET':
        transcripts = Transcript.find_by_index('idx_owned', user.id)
        return template("admin.html", transcripts=transcripts)

    # They are requesting a transcript assignment
    # TODO: actually find and assign the transcript
    return redirect(url_for('admin.admin_page'))


@admin.route('/upload/transcript', methods=['POST'])
@require_login
def upload_transcript():
    # TODO: do something with file
    flash("Transcript was NOT upload", "warning")
    return redirect(url_for('admin.admin_page'))


@admin.route('/upload/taxonomy', methods=['POST'])
@require_login
def upload_taxonomy():
    # TODO: do something with file
    flash("Taxnonomy was NOT upload", "warning")
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
