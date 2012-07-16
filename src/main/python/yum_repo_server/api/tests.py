from django.test.simple import DjangoTestSuiteRunner
import unittest
from teamcity.unittestpy import TeamcityTestRunner, TeamcityTestResult
from testCleanup import TestCleanup
from testCreateRepo import TestCreateRepo
from testRepoConfigService import TestRepoConfigService
from testRpmFile import TestStaticRepo
from testScheduler import TestScheduler
from testStaticRepo import TestStaticRepo
from testUpload import TestLegacyUpload
from testCsvListing import TestCsvListing
from testVirtualRepo import TestVirtualRepo
from testRepoClient import TestRepoClient
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
        
    