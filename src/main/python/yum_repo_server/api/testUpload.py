from yum_repo_server.test.testconstants import Constants
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
import httplib
import unittest

class TestLegacyUpload(BaseIntegrationTestCase):

    def test_legacy_upload(self):
        repo_name = self.createNewRepoAndAssertValid()
        filename = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        f = open(filename,"rb")
        chunk = f.read()
        f.close()
        
        headers = {'Content-Type' : 'application/x-www-form-urlencoded'}
        response = self.doHttpPost(Constants.HTTP_REPO + '/' + repo_name + '/', chunk, headers)
        self.assertEquals(response.status, httplib.CREATED, "Returncode for legacy upload must be CREATED, but was" + str(response.status))
        
if __name__ == '__main__':
    unittest.main()