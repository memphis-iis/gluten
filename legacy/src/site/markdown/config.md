Configuration, Logging, and Security
====================================

Important: this is about *application* configuration.  If you need information
on configuring the server, please
[Deployment (including Server Setup)](deploy.html)

Logging
-------

Application Logging is simple and is based on log4j - see Configuration below.

Web server logging is default.  If you're running a server setup as described
in [Deployment] (deploy.html) then you should just look in /var/log/apache2
and /var/log/tomcat7

Security
--------

Authentication is handled via Google OAuth 2.  What little authorization we
have is handled via roles specified in the configuration file (see also the
Configuration section below).

Generally, see the ServletBase class and how it setups up the role membership
properties for the request attributes for templates (`Const.REQ_IS_*`)

 * Assigner (assign a transcript to someone for tagging) is specified in the
   annotator.properties file
 * Verifier (merge/verify all completed copies of a transcript into a new copy)
   is specified in the annotator.properties file
 * Assessor ("view any two" transcript functionality) is the union of Assigner
   and Verifier

Configuration
-------------

Based on a bundled default overridden by a properties file.  See
`/var/tagging/conf` on the dev and prod servers for examples. Keep in mind that
all properties are sourced from:

 * System environment variables
 * Java system properties (which can be specified on the JVM command line)
 * The default properties as specified in
   `src/main/resources/annotator.default.properties`
 * The user-specified properties supplied in the annotator.properties file
   in the config directory as defined by `annotator.config.dir` (see below)   

The default property file that we bundle in the jar can be viewed in
the project at `src/main/resources/annotator.default.properties`.  However,
a brief description of the properties in that file will be given here:

 * __annotator.config__ - Just a string that is displayed in the logs on startup:
   mainly for troubleshooting config file issues (you can check if the correct
   config file was read by looking for this string in the log files)
 * __annotator.config.dir__ - directory where the "real" config file is located.
   This is generally specified as an environment variable or a Java system
   property
 * __annotator.log4j.config.classpath__ - If true, the log4j config file
   specified by annotator.log4j.config.file is assumed to be a classpath
   resource.  If false or missing, it is assumed to be a file on the file
   system.
 * __annotator.log4j.config.file__ - Name of the log4j config file (in XML
   format) to use for logging.  Note that it can be a filesystem file or a
   resource on the class path based on annotator.log4j.config.classpath. If
   a filesystem file, the absolute path should be specified.    
 * __annotator.transcript.dir__ - base dir where transcripts are stored. On
   our servers this is `/var/tagging/userdata`
 * __annotator.database.location__ - Current *unused*.  Was meant to be a location
   for a derby database (or a JDBC URL if we ever got fancy)
 * __annotator.trainer.name__ - The user name that specifies where training
   transcripts (the "gold standard" information) are stored.  Note that this will
   uniquely determine the location of the gold standard scripts as:
   `${annotator.transcript.dir}/${annotator.trainer.name}/Completed`
 * __annotator.untagged.name__ - Similar to annotator.trainer.name, this property
   specifies the location of untagged transcripts to be assigned to one or more
   users.  The full location (note the status is different from training) is:
   `${annotator.transcript.dir}/${annotator.untagged.name}/Pending`
 * __annotator.admin.verifier__ - HISTORY ONLY - this was a comma-delimited list
   of email addresses authorized for Verify mode.  This (and tagging) is now
   tracked in the file userrole.csv
 * __annotator.admin.assigner__ - Comma-delimited list of email addresses
   authorized for Assign mode
 * There are several oauth2.google properties:
   - __oauth2.google.client.id__ - Assigned by your Google Account dashboard 
   - __oauth2.google.client.secret__ - Assigned by your Google Account dashboard
   - __oauth2.google.scope__ - The scope used to get user info
   - __oauth2.google.auth.uri__ - Authenication URI we use for Google OAuth2
   - __oauth2.google.token.uri__ - Token URI we use for Google OAuth2
   - __oauth2.google.userinfo.uri__ - UserInfo URI we use for Google OAuth2 (note
     that this corresponds to the scope specified by oauth2.google.scope 
 * __annotator.test.user.email__ - Used to specified test data (instead of logging in)
 * __annotator.test.user.name__ - Used to specified test data (instead of logging in)
