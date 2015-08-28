import yaml

from gludb import DBObject, Field


@DBObject(table='Users')
class User(object):
    email = Field('')
    name = Field('')


@DBObject(table='Taxnonomies')
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
    owner = Field('')
    assigned_to = Field('')
    taxonomy = Field('')
