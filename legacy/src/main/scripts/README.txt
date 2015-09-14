src/main/scripts README for Annotator
========================================

This folder contains scripts that you might find useful:

Log Parsing Helpers
----------------------

 * log_parse.py - a small log-parsing utility that can parse the audit logs
   written by Annotator
 
 * act_count.py - a small demo script using log_parse.py

Deployment Helpers
----------------------

(NOTE that ALL of these helpers make some VERY specific assumptions about
file locations, servers used, the underlying platform, and even server names.)

 * fix_tag_perms.sh - Sample script for correcting file permissions for
   transcripts. (Because they often get changed when manually working with the
   files.)

 * dev-build-annotator.sh - a sample script for building Annotator (including
   the Maven site and docs) on a development server from the latest version
 
 * dev-deploy-annotator.sh - a sample script for deploying Annotator on
   a development server.
 
 * prod-deploy-annotator.sh - a sample script for deploying Annotator on a
   production server. It assumes that the WAR file was previously build and
   deployed using the dev-* scripts above.

 * assign/assign.sh - A sample script for reading student emails and
   transcript files from text files and "mass assigning" transcripts.
