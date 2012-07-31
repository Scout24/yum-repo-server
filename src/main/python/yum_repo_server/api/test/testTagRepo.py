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


    def test_tag_repo_fails_with_no_tag_in_postdata(self):
        reponame=self.createNewRepoAndAssertValid()
        response = self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/","hello world how are you today")
        self.assertTrue(response.status, httplib.BAD_REQUEST)
        
    def test_tag_repo_post_ok(self):
        reponame=self.createNewRepoAndAssertValid()
        post_data="tag=tag_"+reponame
        response = self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data)
        self.assertEquals(response.status, httplib.CREATED)

    def test_tag_repo_read_ok(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        post_data="tag="+tag
        self.doHttpPost(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/",post_data)
        response = self.doHttpGet(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/")
        read_tags=response.read()
        self.assertTrue(tag in read_tags)

   
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

