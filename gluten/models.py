import xml.etree.ElementTree as ET
import re

import yaml

from gludb.simple import DBObject, Field, Index


class ValidationError(Exception):
    pass


def _check(condition, errstr):
    if not condition:
        raise ValidationError(errstr)


@DBObject(table_name='Users')
class User(object):
    email = Field('')
    name = Field('')
    photo = Field('/static/anonymous_person.png')
    logins = Field(list)

    @Index
    def idx_email(self):
        return self.email.strip().lower()


@DBObject(table_name='Taxnonomies')
class Taxonomy(object):
    name = Field('')
    tagger_supplied = Field(list)
    modes = Field(list)
    acts = Field(list)

    @classmethod
    def from_yaml(cls, yamlstr):
        data = yaml.load(yamlstr)
        object = cls(
            tagger_supplied=data.get('tagger-supplied', list()),
            modes=data.get('modes', list()),
            acts=data.get('acts', list())
        )
        object.validate()
        return object

    @classmethod
    def from_yaml_file(cls, filename):
        with open(filename, "r") as f:
            return cls.from_yaml(f.read())

    def validate(self):
        """Raise a ValidationError if the current internal state isn't
        correct"""

        # Specifics for tagger-supplied
        _check(
            isinstance(self.tagger_supplied, list),
            "Invalid tagger-supplied data"
        )

        for ts in self.tagger_supplied:
            q = ts.get('question', None)
            _check(isinstance(q, dict), "Invalid tagger-supplied question")
            _check(len(q.get('name', '')) > 0, "tagger question missing name")
            _check(len(q.get('text', '')) > 0, "tagger question missing text")
            a = q.get('answer', None)
            if a:  # Remember, empty is OK
                _check(type(a) is list, "Invalid tagger-supplied answer list")

        # Specifics for modes
        _check(isinstance(self.modes, list), "Invalid modes")
        _check(len(self.modes) > 0, "No modes specified")
        for m in self.modes:
            _check(isinstance(m, str), "Invalid mode")
            _check(len(m) > 0, "Blank mode")

        # Specifics for acts
        _check(isinstance(self.acts, list), "Invalid acts")
        _check(len(self.acts) > 0, "No speech act specified")
        for act in self.acts:
            _check(isinstance(act, dict), "Invalid speech act")
            act = act.get('act', None)
            _check(isinstance(act, dict), "Invalid speech act")

            name = act.get('name', None)
            _check(isinstance(name, str), "Invalid speech act name")

            if name != 'Unspecified':
                st = act.get('subtypes', None)
                _check(isinstance(st, list), "Invalid act subtypes")
                _check(len(st) > 0, "Speech acts missing subtypes")
                for t in st:
                    _check(isinstance(t, str), "Invalid act subtype")
                    _check(len(t) > 0, "Blank act subtype")


@DBObject(table_name='Transcripts')
class Transcript(object):
    STATES = ['Pending', 'InProgress', 'Completed']

    # Our own housekeeping fields
    owner = Field('')              # Uploader with assignment rights
    taxonomy = Field('')           # Opt: an uploaded taxonomy to use
    source_transcript = Field('')  # Original transcript we're copied from
    state = Field(STATES[0])       # One of STATES

    tagger = Field('')             # ID of User record for current tagger
    verifier = Field('')           # ID of User record for current verifier
    tagged_time = Field('')
    verified_time = Field('')

    # Fields specified in the transcript
    script_identifier = Field('')
    begin_datetime = Field('')
    script_duration = Field(0)
    learner_lag_duration = Field(0)
    class_level = Field('')
    domain = Field('')
    area = Field('')
    subarea = Field('')
    problem_from_learner = Field('')
    learner_notes = Field('')
    tutor_notes = Field('')

    # From taxonomy: was soundness, sessionComments, learningAssessmentScore,
    # and learningAssessmentComments in first Annotator. Now is specified
    # dynamically via the taxonomy
    tagger_supplied_answers = Field(dict)

    # Actual transcript
    raw_transcript = Field('')
    utterance_list = Field(list)

    # Indexes

    @Index
    def idx_state(self):
        return self.state

    @Index
    def idx_owned(self):
        return self.owner

    @Index
    def idx_assigned(self):
        return self.tagger

    # Implementation

    def mark_in_progress(self):
        self.state = self.STATES[1]

    def mark_completed(self):
        self.state = self.STATES[-1]

    @classmethod
    def from_xml(cls, xmlstr):
        root = ET.fromstring(xmlstr)
        _check(root.tag == "Session", "Invalid transcript session")

        def read(tagname, defval=''):
            return root.findtext(tagname, default=defval)

        obj = cls(
            script_identifier=read('ScriptId', ''),
            tagger=read('Tagger', ''),
            verifier=read('Verifier', ''),
            begin_datetime=read('BeginDateTime', ''),
            script_duration=float(read('ScriptDuration', 0)),
            learner_lag_duration=float(read('LearnerLagDuration', 0)),
            class_level=read('ClassLevel', ''),
            domain=read('Domain', ''),
            area=read('Area', ''),
            subarea=read('Subarea', ''),
            problem_from_learner=read('ProblemFromLearner', ''),
            learner_notes=read('LearnerNotes', ''),
            tutor_notes=read('TutorNotes', ''),
            raw_transcript=read('Transcript')
        )

        # TODO: need to parse XML Utterances as well so that we can
        #       import/export like the old Annotator

        obj.parse_raw_transcript()
        return obj

    @classmethod
    def from_xml_file(cls, filename):
        with open(filename, "r") as f:
            return cls.from_xml(f.read())

    def parse_raw_transcript(self):
        if self.utterance_list:
            return  # Already done

        txt = self.raw_transcript.strip()
        if not txt:
            return  # Nothing to

        timestamp = re.compile(r'^\[\d\d:\d\d:\d\d\]')

        seen_blank = True  # Yes: default to seen a blank line
        current_speaker = "UNKNOWN SPEAKER"

        for line in txt.split('\n'):
            line = line.rstrip()
            if not line:
                seen_blank = True
                continue

            match = timestamp.match(line)

            if match:
                # Timestamp match - beginning of utterance
                self.utterance_list.append({
                    'timestamp': match.group().strip('[]'),
                    'text': line[match.span()[1]:].strip(),
                    'speaker': current_speaker,
                    'act': '',
                    'subact': '',
                    'mode': '',
                    'comments': '',
                    'tag_confidence': '',
                })
            elif seen_blank and self.speaker_match(line):
                # Found the current speaker
                current_speaker = line.strip()
            elif self.utterance_list:
                # No blank seen and no speaker match, but we have at least one
                # utterance: must be a continuation
                utt = self.utterance_list[-1]
                utt['text'] += '\r\n' + line.strip()

            seen_blank = False

    def speaker_match(self, line):
        line = line.strip().lower()
        if not line:
            return False

        if line == "you" or line.endswith('(tutor)'):
            return True  # tutor
        if line.endswith('(customer)'):
            return True  # customer
        if line == "system message":
            return True  # sys

        return False
