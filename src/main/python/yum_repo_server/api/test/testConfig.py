import unittest
from yum_repo_server.api.config import get_non_deletable_repositories,\
    _valid_repository_name

class TestConfig(unittest.TestCase):
    
    def test_get_deletable_repositories(self):
        self.assertEquals(3, len(get_non_deletable_repositories('src/test/resources/non-deletable-repositories')))

    def test_get_deletable_repositories_ignores_invalid_entries(self):
        self.assertEquals(2, len(get_non_deletable_repositories('src/test/resources/non-deletable-repositories-with-invalid-entries')))
        
    def test_filter_entries_with_slashes(self):
        self.assertFalse(_valid_repository_name('dev/noarch'))
        
    def test_filter_entries_with_spaces(self):
        self.assertFalse(_valid_repository_name('dev test'))
