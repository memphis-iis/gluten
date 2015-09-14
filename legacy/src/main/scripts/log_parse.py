#!/usr/bin/env python

"""log_parse.py - Parsing support for server.logs from the TDC
Annotator tool.  This script accepts command line parameters specifying
log files to read.  All user audit "actions" are tabulated and printed
out.

NOTE that this functionality is really only a sample.  You probably want
to write your own code to use the AnnotatorLogParser class to read your
copy of server.log from iistdc.memphis.edu:

    from log_parse import AnnotatorLogParser
    for entry in AnnotatorLogParser(open('server.log')).read_entries():
        do_something(entry)

Where entry is a dictionary:
    * date - date of the log entry (string)
    * time - time of the log entry (string)
    * threadinfo - simple thread name the message was logged by (string)
    * level - log level (string)
    * src - the log source, generally a class name (string)
    * payload - the actual "text" of the log entry (string)

Depending on the payload, other fields may be present.  Currently "user
audit" log entries get special parsing and result in new fields:

    * user_audit_good - a valid user audit record was found (boolean)
    * useraction - the reported user action (string)
    * userinfo - data specific to the current action (dictionary)
        + usr - user name (generally an email)
        + name - user's full name (may just be the email)
        + state - state of the file (Pending, Completed, etc)
        + baseFn - base file name (without path, etc)
        + srcFn - full path of the file name on the server's file sys
    * userstats - stats about the file specified in userinfo.
      IMPORTANT - although the statistics are actually int's, not effort
      is made to translate them from strings,  In addition, act, subact,
      and mode all mentioned "valid" tags.  A "valid" tag is non-empty
      and is NOT "unspecified"
        + totitems - count of items (utterances) in the transcript
        + act - total number of items with a "valid" dialog act specified
        + subact - total number of items with a "valid" subact specified
        + mode - total number of items with a "valid" dialog mode (change) specified

So if you wanted to generate the number of dialog acts tagged across all
user actions, then the do_something in the sample code above becomes:

    def do_something(entry):
        if entry['user_audit_good']:
            action = entry.get('useraction', '???')
            act_count = entry.get('userstats', {}).get('act', '')
            act_count = int(act_count) if act_count else 0
            #store/yield/return act_count for action somehow

One final note about the log files... In general, there will be 3 files:
server.log, error.log, and audit.log.  For safety, you should be able to
just grab server.log (which has everything) and work from that.  If you
want/need to save time, then you can use the other log files.  Only
messages with a logging level at WARN or worse are in error.log.  Only
log entries with the string '[[AUDIT]]' will be in the audit log.  Note
that the audit log filter doesn't guarantee only audit entries, but it
should be pretty close. In any event, if you're checking for the
'user_audit_good' property as above you won't need to worry about
non-audit entries that happen to contain '[[AUDIT]]'
"""

import sys
import glob
import re
import collections

#Pattern for parsing a line entry from our log4j logs.  Note that this
#is taken from the log4j config file in /var/tagging/conf on
#iistdc.memphis.edu, so if you change that file, you'll need to change
#this pattern
log_line_re = re.compile(
    r'''
    (?P<date>\S+)           #Date
    \s+                     #whitespace
    (?P<time>\S+)           #Time
    \s+                     #whitespace
    (?P<threadinfo>\[.*\]) #threadinfo in []
    \s+                     #whitespace
    (?P<level>\S+)          #level
    \s+                     #whitespace
    (?P<src>\S+)            #src
    \s+                     #whitespace
    -                       #-
    \s+                     #whitespace
    (?P<payload>.*)         #rest of log (i.e. payload)
    ''',
    re.VERBOSE
)

#Pattern for parsing the "payload" section of a log (parsed with
#log_line_re above) that was used for a "user audit" record.  See the
#check for a prefix below and the code in ServletBase in the package
#edu.memphis.iis.tdc.annotator in the Annotator source code (especially
#the userAudit function)
user_audit_re = re.compile(
    r'''
    (?P<useraction>.+)
    :
    \s+
    (?P<userinfo>\{.*\}) #usr,name,state,baseFn,srcFn,etc
    \s+
    (?P<userstats>\{.*\}) #totitems,act,subact,mode
    ''',
    re.VERBOSE
)

def payload_parse(entry):
    """Parse the payload in the log entry.  Note that we return the
    entry parsed, but we also currently modify it in place.  Only "user
    audit" payloads are currently parsed
    """
    if not entry:
        return entry
    payload = entry.get('payload', '')
    if not payload:
        return entry
    
    if payload.startswith('[[AUDIT]]'):
        payload = payload[10:]
        m = user_audit_re.match(payload)
        if m:
            entry.update(m.groupdict())
        else:
            entry['usermsg'] = payload.strip()
    
    return entry

def dict_field(src_dict, field_name):
    """This parses the silly, simple json format we have ({n:v,n:v}).
    We read a string from the given dictionary using the given key as the
    "raw" value.  If the raw value doesn't match our expected format, we
    just return without changing anything.  Otherwise we parse the found
    dictionary, replace the key in src_dict with the parsed dictionary,
    and save the raw value to field_name + '_raw'
    """
    
    raw = src_dict.get(field_name, '')
    if not raw:
        return src_dict
    
    start = raw.find('{')
    if start < 0:
        return src_dict
    
    end = raw.find('}', start)
    if end <= start:
        return src_dict
        
    contents = raw[start+1:end]
    if not contents:
        return src_dict
    
    dct = {}
    for entry in contents.split(','):
        flds = entry.split(':')
        if len(flds) != 2:
            print 'Invalid silly json entry: %s' % str(flds)
            continue
        key,val = flds
        dct[key] = val
    
    src_dict[field_name + "_raw"] = raw
    src_dict[field_name] = dct
    return src_dict

def payload_filter(entry):
    """Perform any necessary changes based on the payload in the entry.
    Note that we also add the field user_audit_good indicating a good
    audit record
    """
    after = payload_parse(entry)
    
    user_audit_good = True
    
    for fld in ['userinfo', 'userstats']:
        after = dict_field(after, fld)
        if not isinstance(after.get(fld, ''), dict):
            user_audit_good = False
    
    after['user_audit_good'] = user_audit_good
    
    return after

class AnnotatorLogParser(object):
    """Conceived as a class since there actually could be state maintained
    line-to-line, but currently just see read_entries
    """
    def __init__(self, line_src):
        self._line_src = line_src
    
    def read_entries(self):
        last_entry = None
        
        #Note possible multi-line messages
        for line in self._line_src:
            line = line.strip() if line else None
            if not line:
                continue
            
            m = log_line_re.match(line)
            if m:
                #Found entry - flush current entry and grab the fields
                if last_entry:
                    yield payload_filter(last_entry)
                    last_entry = None
                last_entry = m.groupdict()
            else:
                #No match - maybe line continuation?
                if last_entry and line:
                    last_entry['payload'] = last_entry.get('payload', '') + '\n' + line
                else:
                    print "Possible Unknown Log Format: %s" % line
        
        #Don't forget final line
        if last_entry:
            yield payload_filter(last_entry)
                    
            
def main():
    filenames = sorted(set([fn for a in sys.argv[1:] for fn in glob.glob(a)]))
    if not filenames:
        print 'No log files to process'

    user_action_counts = collections.defaultdict(int)
    
    for fn in filenames:
        ucount = 0
        print 'READING %s' % fn
        for entry in AnnotatorLogParser(open(fn)).read_entries():
            if entry['user_audit_good']:
                useraction = entry['useraction']
                userinfo = entry['userinfo']
                userstat = entry['userstats']
                
                user_action_counts[useraction] += 1
                
                ucount += 1
                if ucount % 20000 == 0:
                    print 'User actions: %12d' % ucount
                
        print ''
        print 'USER STATS %d' % ucount
    
    for count, action in sorted([(v,k) for k,v in user_action_counts.iteritems()], reverse=True):
        print '%12d %s' % (count, action)
        
if __name__ == "__main__":
    main()
