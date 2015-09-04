"""Utils that we use throughout the application. The most relevant details:

* we keep the storage ID of the current user in session.
* If we need the actual user, we read the current data for that user from
  storage.
* Route handling functions decorated with @require_login can depend on a user
  object being defined on the flask g object.
* The user object is also automatically defined as a top-level variable in the
  context for any template rendered with our template function.
"""

import os.path as pth
import sys
import logging
import traceback

from functools import wraps

from flask import g, url_for, request, redirect, abort, render_template

from gluten.auth import get_user


def app_logger():
    """Centralize a name for the application logger and save other modules the
    trouble of importing logger"""
    return logging.getLogger("gluten")


def project_file(relpath):
    """Given the path to a file relative to the project root, return the
    absolute file name."""
    # Kinda janky - we know this file is one directory up from the project
    # root, so we can work from there
    base = pth.abspath(pth.join(pth.dirname(__file__), '..'))
    return pth.join(base, relpath)


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


def template(template_name, **context_kwrds):
    """Helper that provides any default, base data for our templates. Note that
    it works with @require_login from above"""
    ctx = {
        'user': getattr(g, 'user', None),
        'is_verifier': False,
        'is_assigner': False,
        'is_assessor': False
    }
    ctx.update(context_kwrds)
    return render_template(template_name, **ctx)
