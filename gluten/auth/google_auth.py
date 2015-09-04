"""Flask Blueprint adding login functionality to our app. Note that we expect
gluten model and db config to be handled elsewhere
"""

import sys
import types
import traceback

from functools import partial

from flask import Blueprint, abort, session, url_for, redirect, request
from gludb.utils import now_field

from gluten.utils import app_logger
from gluten.models import User
from gluten.auth import set_user_session
from .flask_oauth import OAuth


oauth = OAuth()

GOOGLE_CLIENT_ID = ''
GOOGLE_CLIENT_SECRET = ''

google = oauth.remote_app(
    'google',
    base_url='https://www.google.com/accounts/',
    authorize_url='https://accounts.google.com/o/oauth2/auth',
    request_token_url=None,
    request_token_params={
        'scope': 'https://www.googleapis.com/auth/userinfo.email',
        'response_type': 'code',
    },
    access_token_url='https://accounts.google.com/o/oauth2/token',
    access_token_method='POST',
    access_token_params={'grant_type': 'authorization_code'},
    bearer_authorization_header=True,
    consumer_key=GOOGLE_CLIENT_ID,
    consumer_secret=GOOGLE_CLIENT_SECRET
)


def _getUserInfo(self):
    return self.get('https://www.googleapis.com/oauth2/v1/userinfo')
google.getUserInfo = types.MethodType(_getUserInfo, google)


auth = Blueprint('auth', __name__)


@auth.route('/login')
def login():
    try:
        set_user_session()  # Clear previous session

        # Figure out any extra parms that we'll send
        extra_params = {}
        redir_url = request.args.get("redir", None)
        if redir_url:
            extra_params['state'] = redir_url

        # Start the oauth process
        callback = url_for('auth.oauthcallback', _external=True)
        return google.authorize(
            callback=callback,
            extra_params=extra_params
        )
    except:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        log = app_logger()
        log.warning("Unexpected error: %s", exc_value)
        log.error(''.join(traceback.format_exception(
            exc_type, exc_value, exc_traceback
        )))
        return abort(500)


@auth.route('/logout')
def logout():
    set_user_session()
    redir_url = request.args.get("redir", None)
    if not redir_url:
        redir_url = '/'
    return redirect(redir_url)


# Simple fail wrapper used below
def _failed_login(redir_url, msg=None):
    session.pop('google_token', None)
    set_user_session()
    if msg:
        app_logger().warning("LOGIN FAILURE:", msg)
    return redirect(redir_url)


@auth.route('/oauthcallback')
@google.authorized_handler
def oauthcallback(resp, user_data=None):
    # No matter what, we need to actually figure out a redirect URL
    redir_url = request.args.get('state', None)
    if not redir_url:
        redir_url = '/'

    fail = partial(_failed_login, redir_url)

    if resp is None:
        return fail()

    try:
        access_token = resp.get('access_token', None)
        if not access_token:
            return fail("OAuth2 Response missing valid access token")

        session['google_token'] = (access_token, '')

        req = google.getUserInfo()
        if req.status != 200:
            return fail("Resp %d: no userinfo" % req.status)

        # Now we can finally get the user data
        data = req.data

        email = data.get('email', '')
        if not email:
            return fail("Google failed to supply an email address")

        users = User.find_by_index('idx_email', email)
        if users:
            user = users[0]
        else:
            user = User(email=email)

        # Update the user info and save the session info
        user.name = data.get('name', email)
        user.photo = data.get('picture', '/static/anonymous_person.png')
        user.logins.append(now_field())
        user.save()

        set_user_session(user.id)
    except:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        log = app_logger()
        log.warning("Unexpected error: %s", exc_value)
        log.error(''.join(traceback.format_exception(
            exc_type, exc_value, exc_traceback
        )))
        return abort(500)

    return redirect(redir_url)


# Used by flask-oauth - note the specific naming... we'll have one
# of these per provider if we ever add anyone other than google
@google.tokengetter
def get_google_token(token=None):
    return session.get('google_token')
