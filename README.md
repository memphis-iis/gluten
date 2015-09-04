# gluten - Transcript Annotator 2.0

A Flask-based Python 3 annotation application.

## Getting started

If you aren't running in a Mac/Linux type environment, you can use the
bundled Vagrantfile (assuming that you have vagrant and VirtualBox installed).
In fact, you can use the Vagrantfile even if you're running on Linux. That way
you don't need to insure you have all the correct dependencies:

    $ vagrant up
    $ vagrant ssh

To run the application locally, you'll need to set up a virtualenv with the
correct dependencies. Assuming you're SSH'ed into the VM (using `vagrant ssh`
as above):

    $ cd gluten
    $ ./setup.sh

Then you need to create a test config file:

    $ touch test.config
    $ nano test.config

At a minimum, you need to supply the Google OAuth credentials that allow users
to log in.  A sample test.config file would look something like this:

    DEBUG = 1
    TEST_EMAIL = 'your_email_here@gmail.com'
    FLASK_SECRET = 'Some big random string'
    GOOGLE_OAUTH_CLIENT_ID = 'get this from google'
    GOOGLE_OAUTH_CLIENT_SECRET = 'get this from google'

You'll want to use your gmail address as the TEST_EMAIL so that when you login,
a few test transcripts will be waiting for you (they're created every time you
start the application). FLASK_SECRET should be a pretty large random string.
GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET should be available at
https://console.developers.google.com/. Be sure to include
http://localhost:5000 under the authorized JavaScript origins, and
http://localhost:5000/auth/authorized under the authorized redirect URL's.
