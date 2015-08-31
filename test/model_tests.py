import unittest

from gluten.models import Taxonomy

from .utils import project_file


class TaxnonmyTesting(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testDefaultRead(self):
        def_tax = project_file('config/default_taxonomy.yaml')
        tax = Taxonomy.from_yaml_file(def_tax)
        tax.validate()
