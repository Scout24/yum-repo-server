import httplib
import unittest
import os
import shutil
import datetime
import time
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
        
    def test_static_listing_lists_repos_without_destination_url_when_parameter_is_given(self):
        reponame = self.createNewRepoAndAssertValid()
        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?showDestination=true")
        response_text = response.read().strip()
        self.assertEquals(reponame, response_text)
        
    def test_virtual_listing_lists_repos(self):
        reponame = unique_repo_name()
        virtualRepoPath = self.config.getVirtualRepoDir()+reponame
        os.makedirs(virtualRepoPath)
        response = self.helper.do_http_get(Constants.HTTP_PATH_VIRTUAL+".txt")
        self.assertTrue(reponame in response.read())
        shutil.rmtree(virtualRepoPath)
        
    def test_virtual_listing_has_destination_url(self):
        static_repo, virtual_repo = self.assert_create_virtual_repo()
        response = self.helper.do_http_get(Constants.HTTP_PATH_VIRTUAL+".txt?showDestination=true")
        response_text = response.read().strip()
        self.assertEquals('%s:/static/%s' % (virtual_repo, static_repo), response_text)
    
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

    def test_filter_by_tags_exclusive(self):
        repo1 = self.createNewRepoAndAssertValid()
        self.repoclient().tagRepo(repo1, "atag")
        self.repoclient().tagRepo(repo1, "atag2")
        repo2 = self.createNewRepoAndAssertValid()
        self.repoclient().tagRepo(repo2, "atag")

        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?notag=atag2")
        self.assertEqual(repo2 + "\n", response.read())

    def test_filter_by_older_then(self):
        repo1 = self.createNewRepoAndAssertValid()
        repo2 = self.createNewRepoAndAssertValid()
        configService = RepoConfigService()
        repo2dir = configService.getStaticRepoDir(repo2)

        self.set_mtime(repo2dir)

        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?older=5")
        self.assertEqual(repo2 + "\n", response.read())

    def test_filter_by_newer_then(self):
        repo1 = self.createNewRepoAndAssertValid()
        repo2 = self.createNewRepoAndAssertValid()
        configService = RepoConfigService()
        repo2dir = configService.getStaticRepoDir(repo2)

        self.set_mtime(repo2dir)

        response = self.helper.do_http_get(Constants.HTTP_PATH_STATIC+".txt?newer=5")
        self.assertEqual(repo1 + "\n", response.read())

    def set_mtime(self, path):
        today = datetime.datetime.now()
        pastday = today - datetime.timedelta(days=11)
        atime = int(time.mktime(pastday.timetuple()))
        os.utime(path, (atime, atime))

if __name__ == '__main__':
    unittest.main()

    