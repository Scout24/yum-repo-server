import httplib
import unittest
import os
import yaml
from yum_repo_client.repoclient import RepoException
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.test import Constants, unique_repo_name
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase


class TestTagRepo(BaseIntegrationTestCase):
    config=RepoConfigService()


    def test_tag_repo_ok(self):
        reponame=self.createNewRepoAndAssertValid()
        post_data="tag=tag_"+reponame
        response = self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data)
        self.assertEquals(response.status, httplib.CREATED)
        os.listdir(self.config.getStaticRepoDir(reponame))
   
    def test_tag_repo_creates_tags_dir(self):
        reponame=self.createNewRepoAndAssertValid()
        post_data="tag=tag_"+reponame
        self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data)
        self.assertTrue(os.path.exists(self.config.getTagsDirForStaticRepo(reponame)))

    def test_tag_repo_writes_tag(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        post_data="tag="+tag
        self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data)
        targetFile=self.config.getTagsFileForStaticRepo(reponame)
        self.assertTrue(self.fileContains(targetFile,tag))

    def test_tag_twice_writes_tags(self):
        reponame=self.createNewRepoAndAssertValid()
        tag1="tag1_"+reponame
        tag2="tag2_"+reponame
        post_data1="tag="+tag1
        post_data2="tag="+tag2
        self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data1)
        self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data2)
        targetFile=self.config.getTagsFileForStaticRepo(reponame)
        self.assertTrue(self.fileContains(targetFile,tag1))
        self.assertTrue(self.fileContains(targetFile,tag2))
    
    def fileContains(self,targetFile,contents):
        f = open(targetFile, "r")
        text = f.read()
        f.close()
        if contents in text:
          return True
        return False
        
if __name__ == '__main__':
    unittest.main()

