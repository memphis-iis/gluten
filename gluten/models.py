import yaml

from gludb.simple import DBObject, Field


class ValidationError(Exception):
    pass


def _check(condition, errstr):
    if not condition:
        raise ValidationError(errstr)


# Useful for checks for string type that work for both Python 2 & 3
try:
    basestring
except NameError:
    basestring = str


@DBObject(table_name='Users')
class User(object):
    email = Field('')
    name = Field('')


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
            _check(len(q.get('text', '')) > 0, "tagger question missing text")
            a = q.get('a', None)
            if a:  # Remember, empty is OK
                _check(type(a) is list, "Invalid tagger-supplied answer list")

        # Specifics for modes
        _check(isinstance(self.modes, list), "Invalid modes")
        _check(len(self.modes) > 0, "No modes specified")
        for m in self.modes:
            _check(isinstance(m, basestring), "Invalid mode")
            _check(len(m) > 0, "Blank mode")

        # Specifics for acts
        _check(isinstance(self.acts, list), "Invalid acts")
        _check(len(self.acts) > 0, "No speech act specified")
        for act in self.acts:
            _check(isinstance(act, dict), "Invalid speech act")
            act = act.get('act', None)
            _check(isinstance(act, dict), "Invalid speech act")

            name = act.get('name', None)
            _check(isinstance(name, basestring), "Invalid speech act name")

            if name != 'Unspecified':
                st = act.get('subtypes', None)
                _check(isinstance(st, list), "Invalid act subtypes")
                _check(len(st) > 0, "Speech acts missing subtypes")
                for t in st:
                    _check(isinstance(t, basestring), "Invalid act subtype")
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
    script_indentifier = Field('')
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
    # and learningAssessmentComments in first Annotator
    tagger_supplied_answers = Field(dict)

    # Actual transcript
    raw_transcript = Field('')
    utterance_list = Field('')

    # TODO: parse xml file
    # TODO: parse raw_transcript into utterance list
    # TODO: some kind of handling for tagger_supplied_answers
