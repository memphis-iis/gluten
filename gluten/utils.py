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
import logging

from flask import g, render_template


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


def template(template_name, **context_kwrds):
    """Helper that provides any default, base data for our templates. Note that
    if g.user is defined (which routes decorated with .auth.require_login will
    have), the user will be added to the context"""
    ctx = {
        'user': getattr(g, 'user', None)
    }
    ctx.update(context_kwrds)
    return render_template(template_name, **ctx)


def first(lst):
    """Return the first element of the given list - otherwise return None"""
    return lst[0] if lst else None
