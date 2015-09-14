Development
===========

Introduction
------------

The project code is housed in a Subversion repository and is Maven based.  It
has been debugged successfully using Eclipse (Kepler) with the Eclipse Maven
plugin.

For any "real" debugging, you'll want to create a test folder for
testing transcripts and such.  See annotator_setup.tar.gz in the setup
directory of the project and the workstation debugging setup section below.

Workstation Debugging Setup
---------------------------

Your development workstation really only needs Java 7, Maven, and Subversion
to get started.  If you have these installed and configured at a command
prompt, you should be able to get setup from scratch with:

    command_prompt> svn checkout http://iistdc-dev.memphis.edu/svn/annotator/trunk annotator
    command_prompt> cd annotator
    command_prompt> mvn install tomcat7:run

However, you'll also want to set up a simple development folder for testing.
On Windows, you should be able to extract it in the directory `c:\tmp`.  On
Linux or Mac OS, extract it to `/tmp`.  For simplicity's sake, let's assume
that you're working on Windows and you just checked out the code as above
to the directory `C:\annotator` *and* you use something like Cygwin or MinGW
for Linux-like command prompt tools:

    command_prompt> mkdir c:\tmp
    command_prompt> cd c:\tmp
    command_prompt> tar -zxf /annotator/setup/annotator_setup.tar.gz

Should be all you need to do to get your environment set up.  Of course, you
can also use your favorite GUI-based archive manager as well. 

Development Server
------------------

The server iistdc-dev is running a development copy of the server
(which is handy since that's also where the build generally happens).
It also hosts the SVN repository and the Maven-generated site (which
you are currently reading). 

Note that you should look at the [Deployment page] (deploy.html)
for the real information on setting up a dev or prod server. 
