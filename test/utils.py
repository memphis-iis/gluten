"""Some simple utilities for testing
"""

import os.path as pth

S3_DIR = '/tmp/s3'
BACKUP_BUCKET_NAME = 'gluten-backups'


def project_file(relpath):
    """Given the path to a file relative to the project root, return the
    absolute file name. We depend on the fact that this file is in the test
    directory, which is one-level down"""
    base = pth.abspath(pth.join(pth.dirname(__file__), '..'))
    return pth.join(base, relpath)
