#!/usr/bin/env python

"""act_count.py - This is really just a simple demo script for working
with the server log from iistdc.memphis.edu.  Note that this could be
considered extra work since all the data needed should be in audit.log.
Also note that file name (and assumption that the file is in the current
working directory) is hard-coded.  Told you it was a demo.
"""

from collections import defaultdict

from log_parse import AnnotatorLogParser

def gather(entry):
    if not entry['user_audit_good']:
        return None, None
    action = entry.get('useraction', '???')
    act_count = entry.get('userstats', {}).get('act', '').strip()
    act_count = int(act_count) if act_count else 0
    return action, act_count

def main():
    action_counts = defaultdict(int)
    actions_act_counts = defaultdict(int)
    
    for entry in AnnotatorLogParser(open('server.log')).read_entries():
        action, act_count = gather(entry)
        if action:
            action_counts[action] += 1
            actions_act_counts[action] += act_count
    
    print "%12s %14s %s" % ("Action Count", "DlgAct/Action", "Action Name")
    for action in sorted(action_counts.keys()):
        action_count = action_counts[action]
        act_total = actions_act_counts[action]
        print "%12d %14.2f %s" % (
            action_count,
            float(act_total) / float(action_count),
            action,
        )

if __name__ == "__main__":
    main()

