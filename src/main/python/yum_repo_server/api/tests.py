from django.test.simple import DjangoTestSuiteRunner
import unittest
from teamcity.unittestpy import TeamcityTestRunner, TeamcityTestResult
from test.testCleanup import TestCleanup
from test.testCreateRepo import TestCreateRepo
from test.testRpmFile import TestStaticRepo
from test.testScheduler import TestScheduler
from test.testTagRepo import TestTagRepo
from test.testStaticRepo import TestStaticRepo
from test.testUpload import TestLegacyUpload
from test.testCsvListing import TestCsvListing
from test.testVirtualRepo import TestVirtualRepo
from test.testRepoClient import TestRepoClient
from yum_repo_server.api.test.testRpmPropagation import TestRpmPropagation
from yum_repo_server.api.services.test.tests import *
from teamcity_test_runner_extension.teamcityTestRunner import ExtendedTeamcityTestRunner

class TeamCityDjangoTestSuiteRunner(DjangoTestSuiteRunner):


    def run_suite(self,suite, **kwargs):
        test_result = ExtendedTeamcityTestRunner().run(suite)
        test_result.printResults()
        
        return test_result
        
    
