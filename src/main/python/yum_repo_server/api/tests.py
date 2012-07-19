from django.test.simple import DjangoTestSuiteRunner
import unittest
from teamcity.unittestpy import TeamcityTestRunner, TeamcityTestResult
from test.testCleanup import TestCleanup
from test.testCreateRepo import TestCreateRepo
from test.testRepoConfigService import TestRepoConfigService
from test.testRpmFile import TestStaticRepo
from test.testScheduler import TestScheduler
from test.testStaticRepo import TestStaticRepo
from test.testUpload import TestLegacyUpload
from test.testCsvListing import TestCsvListing
from test.testVirtualRepo import TestVirtualRepo
from test.testRepoClient import TestRepoClient
from yum_repo_server.api.test.testRpmPropagation import TestRpmPropagation
from teamcity_test_runner_extension.teamcityTestRunner import ExtendedTeamcityTestRunner

class TeamCityDjangoTestSuiteRunner(DjangoTestSuiteRunner):


    def run_suite(self,suite, **kwargs):
        # just for local development
        #suite1 =  unittest.TestLoader().loadTestsFromTestCase(TestStaticRepo)
        #suite2 = unittest.TestLoader().loadTestsFromTestCase(TestVirtualRepo)
        #suite3 = unittest.TestLoader().loadTestsFromTestCase(TestCreateRepo)
        #suite4 = unittest.TestLoader().loadTestsFromTestCase(TestScheduler)
        #suite5 = unittest.TestLoader().loadTestsFromTestCase(TestRepoConfigService)
        #suite6 = unittest.TestLoader().loadTestsFromTestCase(TestCsvListing)
        #suite = suite6
        test_result = ExtendedTeamcityTestRunner().run(suite)
        test_result.printResults()
        
        return test_result
        
    