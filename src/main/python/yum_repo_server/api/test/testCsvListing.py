import httplib
import unittest
import os
import shutil
from yum_repo_client.repoclient import RepoException
from yum_repo_server.test import Constants, unique_repo_name
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from yum_repo_server.api.services.repoConfigService import RepoConfigService


class TestCsvListing(BaseIntegrationTestCase):
    
    config = RepoConfigService()
    
    def test_static_listing_is_200_ok(self):
        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt")
        self.assertEquals(httplib.OK,response.status)
        
    def test_virtual_listing_is_200_ok(self):
        response = self.helper.do_http_get(Constants.HTTP_PATH_VIRTUAL+".txt")
        self.assertEquals(httplib.OK,response.status)
        
    def test_does_not_list_invalid_repos(self):
        response = self.helper.do_http_get(Constants.HTTP_PATH_VIRTUAL+"sldknlkdnlnsd.txt")
        self.assertEquals(httplib.BAD_REQUEST,response.status)
        
    def test_static_listing_lists_repos(self):
        reponame = self.createNewRepoAndAssertValid()
        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt")
        self.assertTrue(reponame in response.read())
        shutil.rmtree(self.config.getStaticRepoDir(reponame))
        
    def test_virtual_listing_lists_repos(self):
        reponame = unique_repo_name()
        virtualRepoPath = self.config.getVirtualRepoDir()+reponame
        os.makedirs(virtualRepoPath)
        response = self.helper.do_http_get(Constants.HTTP_PATH_VIRTUAL+".txt")
        self.assertTrue(reponame in response.read())
        shutil.rmtree(virtualRepoPath)
    
    def test_filter_by_name(self):
        self.createNewRepoAndAssertValid()
        reponame = 'some-app-321.1.1-' + unique_repo_name()
        self.repoclient().createStaticRepo(reponame)
        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?name=some-app-.*")
        self.assertEqual(reponame + "\n", response.read())
    
    def test_filter_by_tags_inclusive(self):
        repo1 = self.createNewRepoAndAssertValid()
        self.repoclient().tagRepo(repo1, "atag")
        repo2 = self.createNewRepoAndAssertValid()
        self.repoclient().tagRepo(repo2, "atag")
        self.repoclient().tagRepo(repo2, "atag2")
        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?tag=atag2,btag")
        self.assertEqual(repo2 + "\n", response.read())


if __name__ == '__main__':
    unittest.main()

    