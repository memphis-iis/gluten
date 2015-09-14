Introduction and Architectural Overview
=======================================

Introduction
------------

Annotator is a simple web-based application for annotating a series of
utterances in a transcript file.

Please note that most project documentation is in a
[Google Drive folder][docs]. Please contact Chip at <chipmorrison@gmail.com>
if you need access to the folder. 

Architecture
------------

The server side is written in Java (and assumes at least Java 7). It is
designed to run in all common Java web containers (and is deployed to Tomcat
7).  The client side HTML leverages Bootstrap for CSS and jQuery (and friends)
for JavaScript.  The project is Maven-based, so see pom.xml for details.

In general, a very pared down MVC (Model-View-Controller) design has been used.
In addition, a fairly simple configuration context class has been used instead
of a full-blown Dependency Injection framwork like Spring.

[docs]: https://drive.google.com/folderview?id=0B7FhlYQzRdBsdFBVajhiV3FLa0k&usp=sharing

File/Data Handling
-------------------

Transcript (tagged or not) are stored on the filesystem (at this point in
`/var/tagging/userdata`).  The exact location of a file is determined by
a user name (we use email address as taken from a Google OAuth2 login), the
current state/status of the file (Pending, InProgress, or Completed), and
the file name.  It is assumed that the file name for a transcript uniquely
identifies a transcript. Thus `cp dir1/t1.xml dir2/t1.xml` creates a copy
of an existing transcript while `cp dir/t1.xml dir/t2.xml` creates a
**brand new** transcript.

Transcripts are in XML files with a textual representation of the transcript:

    Tutor
    [00:00:01] Hello World
    
    Student
    [00:01:05] Good Night Moon
    
The first time a transcript is read from the filesystem, the transcript text
is parsed into "transcript items" (see the class Utterance) that contain the
speaker, timestamp, text, and any tagging data to this point.  Those items
are always written back to the file.

Training Mode
--------------

If a transcript is opened by a user, the "training" user is checked.  If there
is a completed copy of the current transcript then it is opened in "training"
mode.  The training tags are available when a button is clicked and the GUI
visually indicates when rows are annotated differently from the training data.

Administrative Functions
------------------------

Currently there are only a few administrative functions:

 * __Assign__ - allows you to copy transcript files within the system. (e.g.
   from untagged/Pending to a user's Pending folder to assign a transcript) 
 * __Verify__ - allows you to create a single merged copy for editing from all
   completed copies of a file.
 * __Assess__ - allow you to open any transcript in read-only mode.  An optional
   "backing" transcript can also be opened for comparison
