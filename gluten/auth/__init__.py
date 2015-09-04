from flask import session

from gluten.models import User


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
