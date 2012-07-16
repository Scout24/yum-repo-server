from teamcity.unittestpy import TeamcityTestRunner, TeamcityTestResult
from setuptools.command.test import test
from django.utils.unittest.loader import TestLoader

class ExtendedTeamcityTestRunner(TeamcityTestRunner):
    def _makeResult(self):
        return ExtendedTeamcityResult(self.stream)
    

class ExtendedTeamcityResult(TeamcityTestResult):
    
    def startTest(self, test):
        self.testsRun += 1
        TeamcityTestResult.startTest(self, test)
    
    def printResults(self):
        self.output.write('\nTests run: %i, Failures: %i, Errors %i \n\n' % (self.testsRun, len(self.failures), len(self.errors)))
        self._print_test_names_of_list_as(self.failures, 'Failures')
        self._print_test_names_of_list_as(self.errors, 'Errors')
        
    def _print_test_names_of_list_as(self, tests, label):
        if len(tests) > 0:
            self.output.write('%s:\n' % label)
            for failure in tests:
                self.output.write('\t%s\n' % self.getTestName(failure[0]))
            self.output.write('\n\n')

class TeamcityTestRunnerCommand(test):
    def run(self, *args, **kwargs):
        testloader = TestLoader()
        
        testSuite = testloader.discover(self.test_suite, top_level_dir='src/main/python')
        test_result = ExtendedTeamcityTestRunner().run(testSuite)
        test_result.printResults()
        
        if test_result.wasSuccessful():
            exit(0)
        else:
            exit(1)
        
        