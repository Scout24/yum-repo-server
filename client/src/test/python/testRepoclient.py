import os
import unittest

from yum_repo_client.commandline import DefaultConfigLoader


class TestDefaultConfigLoader(unittest.TestCase):
    def test_default_hostname(self):
        os.environ['YUM_REPO_CLIENT_CONFIG'] = 'src/test/resources/yum-repo-client.yaml'
        extractor = DefaultConfigLoader()

        self.assertEqual('test-domain.com', extractor.hostname, 'default hostname should be test-domain.com')

    def test_default_port(self):
        os.environ['YUM_REPO_CLIENT_CONFIG'] = 'src/test/resources/yum-repo-client.yaml'
        extractor = DefaultConfigLoader()

        self.assertEqual(8123, extractor.port, 'default port should be 8123 not %i' % extractor.port)

