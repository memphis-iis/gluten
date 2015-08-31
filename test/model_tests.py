import unittest

from gluten.models import Taxonomy, Transcript

from .utils import project_file


class TaxonomyTesting(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testDefaultRead(self):
        def_tax = project_file('config/default_taxonomy.yaml')
        tax = Taxonomy.from_yaml_file(def_tax)
        tax.validate()


class TranscriptTesting(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testParseFull(self):
        xml_file = project_file('test/sample/CompletedTranscript.xml')
        script = Transcript.from_xml_file(xml_file)

        self.assertEquals('42', script.script_identifier)
        self.assertEquals('testTagger', script.tagger)
        self.assertEquals('testVerifier', script.verifier)
        self.assertEquals(
            '2013-11-17T20:43:04.683-05:00',
            script.begin_datetime
        )
        self.assertEquals(150.28, script.script_duration)
        self.assertEquals(0.08, script.learner_lag_duration)
        self.assertEquals('MAT/116', script.class_level)
        self.assertEquals('Live Math Tutoring - MAT/116', script.domain)
        self.assertEquals('Linear Equations', script.area)
        self.assertEquals('Solving Linear Equations', script.subarea)
        self.assertEquals(
            'confused on the gallons',
            script.problem_from_learner
        )
        self.assertEquals('This tutor is great', script.learner_notes)
        self.assertEquals('This student is great', script.tutor_notes)

        utt = script.utterance_list[1]
        self.assertEquals('00:00:00', utt['timestamp'])
        self.assertEquals('KIMBERLY (Customer)', utt['speaker'])
        self.assertEquals('confused on the gallons', utt['text'])

        utt = script.utterance_list[-1]
        self.assertEquals('02:30:13', utt['timestamp'])
        self.assertEquals('You', utt['speaker'])
        self.assertEquals('bye', utt['text'])
