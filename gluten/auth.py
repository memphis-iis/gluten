"""Flask Blueprint adding login functionality to our app. Note that we expect
gluten model and db config to be handled elsewhere
"""

import sys
import traceback

from functools import partial, wraps

from flask import redirect, request, flash, session, abort, g, url_for
from flask.globals import LocalProxy, _lookup_app_object

try:
    from flask import _app_ctx_stack as stack
except ImportError:
    from flask import _request_ctx_stack as stack

from flask_dance.consumer import (
    OAuth2ConsumerBlueprint,
    oauth_authorized,
    oauth_error
)

from gludb.utils import now_field

from .utils import app_logger
from .models import User


def set_user_session(user_id=None):
    if not user_id:
        user_id = ''
    session['user_id'] = user_id


def get_user():
    """Return current user"""
    user_id = session.get('user_id', '')
    if not user_id:
        return None  # Not logged in
    return User.find_one(user_id)


def require_login(func):
    """Simple decorator helper for requiring login on functions decorated with
    flask route: make sure that it's LAST in the decorator list so that the
    flask magic happens (see voice_testing for an example).

    Important: we are assuming the blueprint endpoint auth.login exists
    """
    @wraps(func)
    def wrapper(*args, **kwrds):
        try:
            user = get_user()
            if user:
                setattr(g, 'user', user)
                return func(*args, **kwrds)
            else:
                url = url_for('auth.login', redir=request.url)
                return redirect(url)
        except:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            log = app_logger()
            log.warning("Unexpected error: %s", exc_value)
            log.error(''.join(traceback.format_exception(
                exc_type, exc_value, exc_traceback
            )))
            return abort(500)

    return wrapper


# Make the google blueprint (taken from their contrib code)
auth = OAuth2ConsumerBlueprint(
    "auth",
    __name__,
    client_id=None,  # Handled via app config
    client_secret=None,  # Handled via app config
    scope=["profile", "email"],
    base_url="https://www.googleapis.com/",
    authorization_url="https://accounts.google.com/o/oauth2/auth",
    token_url="https://accounts.google.com/o/oauth2/token",
    redirect_url=None,
    redirect_to=None,
    login_url=None,
    authorized_url=None,
    authorization_url_params={},
    session_class=None,
    backend=None,
)

auth.from_config["client_id"] = "GOOGLE_OAUTH_CLIENT_ID"
auth.from_config["client_secret"] = "GOOGLE_OAUTH_CLIENT_SECRET"


@auth.before_app_request
def set_applocal_session():
    ctx = stack.top
    ctx.google_oauth = auth.session

google_api = LocalProxy(partial(_lookup_app_object, "google_oauth"))


def login_fail(msg):
    flash(msg, category="error")
    app_logger().error(msg)
    return False


# create/login local user on successful OAuth login
@oauth_authorized.connect
def log_in_event(blueprint, token):
    set_user_session()  # Clear previous session

    if not token:
        return login_fail("Failed to log in")

    resp = blueprint.session.get("/oauth2/v1/userinfo")
    if not resp.ok:
        return login_fail("Failed to login user!")

    data = resp.json()

    email = data.get('email', '')
    if not email:
        return login_fail("Google failed to supply an email address")

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
    app_logger().info("Logged in user id %s, email %s" % (user.id, user.email))


# notify on OAuth provider error
@oauth_error.connect
def github_error(blueprint, error, error_description=None, error_uri=None):
    login_fail("OAuth login failure: [%s] %s (uri=%s)" % (
        error, error_description, error_uri
    ))


@auth.route('/logout')
def logout():
    set_user_session()
    redir_url = request.args.get("redir", None)
    if not redir_url:
        redir_url = '/'
    return redirect(redir_url)
