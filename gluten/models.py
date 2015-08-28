import yaml

from gludb.simple import DBObject, Field


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
            tagger_supplied=data.get('tagger_supplied', list()),
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
        pass  # TODO: actual validation


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
