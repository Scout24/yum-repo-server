import httplib
import unittest
import os
import shutil
from yum_repo_client.repoclient import RepoException
from yum_repo_server.test.testconstants import Constants
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from yum_repo_server.api.services.repoConfigService import RepoConfigService


class TestCsvListing(BaseIntegrationTestCase):
    
    config = RepoConfigService()
    
    def test_static_listing_is_200_ok(self):
        response = self.doHttpGet(Constants.HTTP_PATH_STATIC+".txt")
        self.assertEquals(httplib.OK,response.status)
        
    def test_virtual_listing_is_200_ok(self):
        response = self.doHttpGet(Constants.HTTP_PATH_VIRTUAL+".txt")
        self.assertEquals(httplib.OK,response.status)
        
    def test_does_not_list_invalid_repos(self):
        response = self.doHttpGet(Constants.HTTP_PATH_VIRTUAL+"sldknlkdnlnsd.txt")
        self.assertEquals(httplib.BAD_REQUEST,response.status)
        
    def test_static_listing_lists_repos(self):
        reponame = self.createNewRepoAndAssertValid()
        response = self.doHttpGet(Constants.HTTP_PATH_STATIC+".txt")
        self.assertTrue(reponame in response.read())
        shutil.rmtree(self.config.getStaticRepoDir(reponame))
        
    def test_virtual_listing_lists_repos(self):
        reponame = self.uniqueRepoName()
        virtualRepoPath = self.config.getVirtualRepoDir()+reponame
        os.makedirs(virtualRepoPath)
        response = self.doHttpGet(Constants.HTTP_PATH_VIRTUAL+".txt")
        self.assertTrue(reponame in response.read())
        shutil.rmtree(virtualRepoPath)
    
    

    
   
if __name__ == '__main__':
    unittest.main()

    