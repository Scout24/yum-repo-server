import os.path
from yum_repo_server.rpm.rpmfile import RpmFile

from django.test.testcases import TransactionTestCase
from yum_repo_server.test import Constants

class TestStaticRepo(TransactionTestCase):
    
    def test_read_rpm_file(self):
        basePath = '.'
        if os.path.abspath(basePath).endswith('yum_repo_server/api'):
            basePath = '../../../../..'
        
        basePath = os.path.abspath(basePath) + '/'
        
        path = basePath + Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        rpm = RpmFile(path)
        self.assertEquals(rpm.name, "test-artifact")
        self.assertEquals(rpm.version, "1.2")
        self.assertEquals(rpm.release, "1")
        self.assertEquals(rpm.os, "linux")
        self.assertEquals(rpm.arch, "noarch")
        self.assertEquals(rpm.platform, "noarch-redhat-linux-gnu")
        self.assertEquals(rpm.binary, True)
        


