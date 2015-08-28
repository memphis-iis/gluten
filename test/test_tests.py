"""Some unit tests for our test helpers
"""

import unittest

from .utils import project_file


class UtilTesting(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_project_file(self):
        with open(project_file(__file__), "r") as f:
            txt = f.read()
        our_docstring = globals()['__doc__']
        # Don't forget we read the triple-quote from the file
        self.assertEquals(our_docstring[0:30], txt[3:33])
