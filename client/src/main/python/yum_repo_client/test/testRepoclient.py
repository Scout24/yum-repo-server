import os
import unittest

from yum_repo_client.repoclient import CommandLineClientOptionsExtractor,\
    OptionParsingException

class TestCommandLineClientOptionsExtractor(unittest.TestCase):
    
    def test_extract_and_remove_options_from_arguments(self):
        
        arguments=['path', '--hostname=any-yum-server.de', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        
        self.assertEqual(3, len(arguments), 'option --hostname should have been extracted and removed from list')
        self.assertEqual('any-yum-server.de', extractor.hostname, 'extracted hostname %s is not correct.' % extractor.hostname)

    def test_default_hostname(self):
        os.environ['YUM_REPO_CLIENT_CONFIG'] = 'src/test/resources/yum-repo-client.yaml'
        arguments=['path', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        
        self.assertEqual('test-domain.com', extractor.hostname, 'default hostname should be test-domain.com')
    
    def test_default_port(self):
        os.environ['YUM_REPO_CLIENT_CONFIG'] = 'src/test/resources/yum-repo-client.yaml'
        arguments=['path', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        
        self.assertEqual(8123, extractor.port, 'default port should be 8123 not %i' % extractor.port)
            
    
    def test_port_extraction(self):
        arguments=['path', '--port=8080', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        
        self.assertEqual(3, len(arguments), 'option --port should have been extracted and removed from list')
        self.assertEqual(8080, extractor.port, 'expected port to be 8080 not %i' % extractor.port)
        
    def test_ValueError_when_port_is_not_an_int(self):
        arguments=['path', '--port=8b0', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        self.assertRaises(OptionParsingException, extractor.extract_and_remove_options_from_arguments, arguments)
    
    def test_do_not_remove_unknown_options(self):
        arguments=['path', '--foo=8b0', 'create', 'test-dev']
            
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        
        self.assertEqual(4, len(arguments), 'option --foo should not have been extracted and removed from list')
        
    def test_use_first_equality_sign_for_key_value_split_and_take_the_rest_as_value(self):
        arguments=['path', '--hostname=any-yum=server', 'create', 'test-dev']
        
        extractor = CommandLineClientOptionsExtractor()
        extractor.extract_and_remove_options_from_arguments(arguments)
        self.assertEqual('any-yum=server', extractor.hostname, 'Except the first equality sign, take the rest as value')
    
    def test_option_should_have_an_equality_sign(self):
        arguments=['path', '--hostname', 'any-yum-server', 'create', 'test-dev']
        
        try:
            extractor = CommandLineClientOptionsExtractor()
            extractor.extract_and_remove_options_from_arguments(arguments)
            self.fail('An equality sign is necessary for option/argument assignment.')
        except OptionParsingException:
            pass

