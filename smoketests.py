import unittest
import os
import httplib
import sys

sys.path.append('src/main/python')
sys.path.append('client/src/main/python')


from yum_repo_client.repoclient import HttpClient
from yum_repo_server.test.integrationTestHelper import IntegrationTestHelper
from yum_repo_server.test import unique_repo_name


class TestRemoteServer(unittest.TestCase):
    
    HOST_NAME = os.environ['TESTSERVER_NAME']
    PORT = int(os.environ.get('TESTSERVER_PORT', 80))
    
    def test_remote_server(self):
        repoclient = HttpClient(self.HOST_NAME, self.PORT) 
        
        helper = IntegrationTestHelper(self.HOST_NAME, self.PORT)
        self.assertEquals(httplib.OK, helper.do_http_get('/repo/').status, 'returned response was not ok (200).')
        
        repo_name = unique_repo_name()
        repoclient.createStaticRepo(repo_name)
        repoclient.uploadRpm(repo_name, 'src/test/resources/test-artifact.rpm')
        
        path_to_rpm = "/repo/%s/noarch/test-artifact-1.2-1.noarch.rpm" % (repo_name)
        rpm_url_http_code = helper.do_http_get(path_to_rpm).status
        self.assertTrue(rpm_url_http_code == httplib.OK or rpm_url_http_code == httplib.FOUND)

        repoclient.generateMetadata(repo_name)
        rpm_url_http_code = helper.do_http_get('/repo/%s/repodata/repomd.xml' % repo_name).status
        self.assertTrue(rpm_url_http_code == httplib.OK or rpm_url_http_code == httplib.FOUND)

if __name__ == '__main__':
    unittest.main()
