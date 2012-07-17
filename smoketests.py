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

if __name__ == '__main__':
    unittest.main()