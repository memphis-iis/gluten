Deployment (including Server Setup)
===================================

Quick reminder:

 * Dev server is iistdc-dev.memphis.edu
 * Prod server is iistdc.memphis.edu

Initial Server Setup
--------------------

The essential steps are:

 - If you haven't already, create a Unix group to use for permissions
   (`sudo adduser tagging`)
 - Optionally extract the helpful admin scripts to your user directory from
   `/var/backup/user-help.tar.gz` (the dev and prod servers each have a 
   different copy because the scripts need to be slightly different).
   ***IMPORTANT NOTE:*** you can also sample versions of these scripts in the
   source code in src/main/scripts.
 - Setup and configure a directory for Annotator
   (`sudo mkdir /var/tagging && sudo chown :tagging /var/tagging`)
 - Setup Apache2 to proxy requests to Tomcat for the application (/annotator
   should be proxied to localhost:8080/annotator - see the Apache section
   below)
 - Configure Tomcat (see the Tomcat section below):
     - server.xml should be changed to accomodate the proxy from Apache *and* to
       listen only on 127.0.0.1
     - setenv.sh should be created to set CATALINA_OPTS to identify the
       annotator configuration directory (generally /var/tagging/conf)

Directory Permissions
---------------------

Remember that our app runs under Tomcat, so /var/tagging and all its descendents
should be owned by user `tomcat7` and group `tagging`.  In addition, group
members should have write permissions.  See the script `fix_tag_perms.sh` for
help (described below in User Help Scripts)

User Help Scripts
------------------

There are user help scripts provided on both the dev and prod servers in the
archive `/var/backup/user-help.tar.gz`.  They are:

 * build_annotator.sh - (Dev server only) - will checkout the latest code from
   trunk in the SVN repository and run a full Maven build (including site
   generation) in the directory ~/annotator
 * deploy_annotator.sh - (Dev and Prod, but different on each).  On dev, deploys
   to Tomcat (which includes a Tomcat restart) from the directory ~/annotator
   (which is assumed to have been built by build_annotator.sh above) *and*
   copies the WAR file to the prod server at ~/annotator.war.  On the prod
   server, deploys the copied war to Tomcat 
 * fix_tag_perms.sh - (Dev and Prod) - fixes all tag permissions in /var/tagging
 * assign/assign.sh - (Prod Only) assigns the transcripts in `to_assign.txt` to
   the students listed in `students.txt`
   
See the Deployment section below for how you can use these scripts.

Server Backup
--------------

Both the dev and prod server are backing up to the local location `/var/backup`
on a nightly basis via a cron job that executes `/var/backup/backup.sh`.  Protip:
`sudo crontab -e` to change the cron schedule (note the use of sudo because the
backup runs as root)
     
Apache Config File Changes
--------------------------

To see the changes made on the dev server to set up SVN, please see
`/etc/apache2/mods-enabled/dav_svn.conf`

The main change to the default Apache config is to set up proxying back to the
Tomcat instance.  `/etc/apache2/sites-enabled/000-default` was modified to
include:

    <Location /annotator/>
        ProxyPass http://localhost:8080/annotator/
        ProxyPassReverse http://localhost:8080/annotator/
    </Location> 

See below for the corresponding Tomcat changes

Tomcat Config File Changes
--------------------------

The Tomcat connector should be changed to only listen on address 127.0.0.1 (so
only our Apache server is exposed to the world).  In addition, we need to make
sure Tomcat knows it is behind the Apache proxy.  All the changes can be made
in `/etc/tomcat7/server.xml`

    <Connector port="8080" protocol="HTTP/1.1"
        connectionTimeout="20000"
        address="127.0.0.1"
        proxyName="iistdc-dev.memphis.edu"
        proxyPort="80"
        URIEncoding="UTF-8"
        redirectPort="8443" />

You also need to specify the Annotator config directory.  Generally this is
done via an environment variable and we can do that for tomcat by adding a
setenv.sh file in `/usr/share/tomcat7/bin`.  Its contents should look something
like this:

    #! /bin/sh
    export CATALINA_OPTS="$CATALINA_OPTS -Dannotator.config.dir=/var/tagging/conf"

Although you might want to add these options:
`-Djava.awt.headless=true -Dfile.encoding=UTF-8 -server -Xms1024m -Xmx2048m`


Deployment
----------

The basic workflow for deploying the application is:

 * Check all your work on your local workstation in to SVN
 * Log on to the dev server and build(you should use `build_annotator.sh`
   from the User Help Scripts)
 * Deploy on the dev server and stage to the prod server (using
   `deploy_annotator.sh` on the dev server)
 * Smoke test the dev server (look at transcripts, make sure you haven't broken
   anything, etc)
 * If all is well, log in to the prod server and deploy the WAR file staged 
   above by running `deploy_annotator.sh`
