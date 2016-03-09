# gluten - Transcript Annotator 2.0

A Flask-based Python 3 annotation application.

## Important Legacy note

We maintain a copy of the "legacy" Java application that predated this current
project in the folder `legacy`. However it is NOT maintained. If you are interested
in a Java web application using file-based storage for transcripts, this might be
for you. Let us know and if interest is sufficient we might consider spinning out
a separate project. Otherwise use at your own peril. See also this PR for why you
might want to proceed with caution: https://github.com/memphis-iis/gluten/pull/1

## Getting started

If you are running on a Ubuntu-like environment, you need to make sure you've
installed libyaml-dev. If you have, you can run setup.sh, provide a test.config
file.

    $ sudo apt-get install libyaml-dev
    $ git clone https://github.com/memphis-iis/gluten
    $ cd gluten
    $ ./setup.sh

After that you can run a local debug server with `./local.sh`.

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

## Using Vagrant

We provide a Vagrantfile and provisioning scripts for testing/debugging
gluten locally.  Here are some things to keep in mind...

_IMPORTANT_ - The helper scripts in gluten try to pollute your workstation,
so all Python virtualenv's are created in the directory tree at well-known
places (which are in the .gitignore file so that they aren't checked in to
source code control). This combined with Python's __pycache__ directories and
.pyc files means that you should NOT run gluten both with and without Vagrant
on local workstation. Choose one or the other. Or if you know what you're doing
clean out the virtualenv's and Python bytecode files before you switch. You
have been warned.

If you are familiar with vagrant, you need to know that we create a link
from /vagrant to ~/gluten. If you are NOT familiar with vagrant, you
create our VM, log into it, and then access the code like this:

    $ vagrant up
    $ vagrant ssh
    $ cd gluten

READ the README.md file!!! You need to supply a test.config file before
you can really test things out.

You can use your favorite code editor and version control application in
the host operating system - you can just use this little login to test,
start, or stop the application.

First things first: log in and set up the test environment

   $ vagrant ssh
   $ cd gluten
   $ ./setup.sh

To run the server in development mode:

    $ vagrant ssh
    $ cd gluten
    $ ./local.sh

To run unit tests:

    $ vagrant ssh
    $ cd gluten
    $ ./run_tests.sh

Connect to gluten from your host operating system at:

    http://localhost:5000/

If for some reason you wanted to run mock AWS services in the background (e.g.
better simulate AWS testing):

   $ vagrant ssh
   $ cd gluten
   $ test/local_test_services.sh


## Deploying to AWS Elastic Beanstalk

The deployment is fairly easy:

* Make sure your have a prod.config file created with at least these
  variables defined:
    - DEBUG (should be 0)
    - FLASK_SECRET (use a really good random string)
    - GOOGLE_OAUTH_CLIENT_ID (you get this from Google)
    - GOOGLE_OAUTH_CLIENT_SECRET (you get this from Google)
* Use `./make_aws_upload.sh` to create the ZIP'ed application bundle
* When you create your AWS environment/application, be sure to select:
    - The Python application type
    - A single instance web application (instead of the default load balanced)
    - Use t2.micro instead of t1.micro (t1.micro is the default, but t2.micro
      is cheaper and has more resources)
    - Set the GLUTEN_CONFIG_FILE environment variable to "prod.config"
* Once everything is running, don't forget to add the new URL to the authorized
  list under your Google Developer's console. If you just use a default URL
  from EB, it will look something like:
  http://my-cool-env-name.elasticbeanstalk.com/auth/authorized
