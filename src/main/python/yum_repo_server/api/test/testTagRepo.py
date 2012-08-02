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
        tag="tag_"+reponame
        response = self.repoclient().tagRepo(reponame,tag)
        self.assertEquals(response.status, httplib.CREATED)

    def test_tag_repo_read_ok(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        self.repoclient().tagRepo(reponame,tag)
        response = self.repoclient().tagList(reponame)
        read_tags=response.read()
        self.assertTrue(tag in read_tags)

   
    def test_tag_repo_writes_tag(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        self.repoclient().tagRepo(reponame,tag)
        targetFile=self.config.getTagsFileForStaticRepo(reponame)
        self.assertTrue(self.fileContains(targetFile,tag))



    def test_simple_tag_untag_repo(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        self.repoclient().tagRepo(reponame,tag)
        targetFile=self.config.getTagsFileForStaticRepo(reponame)
        self.assertTrue(self.fileContains(targetFile,tag))
        response=self.repoclient().untagRepo(reponame,tag)
        self.assertEquals(response.status, httplib.NO_CONTENT)
        self.assertFalse(self.fileContains(targetFile,tag))

    def test_many_tags_untag(self):
        reponame=self.createNewRepoAndAssertValid()
        for i in range(10):
          self.repoclient().tagRepo(reponame,str(i))
          self.assertTrue(str(i) in self.repoclient().tagList(reponame).read())
        targetFile=self.config.getTagsFileForStaticRepo(reponame)
        self.assertTrue(self.fileContains(targetFile,str(5)))
        response=self.repoclient().untagRepo(reponame,str(5))
        self.assertEquals(response.status, httplib.NO_CONTENT)
        self.assertFalse(self.fileContains(targetFile,str(5)))
        for i in range(10):
          if not i==5:
            self.assertTrue(self.fileContains(targetFile,str(i)))
    
    def test_untag_tag_fails_if_tag_not_present(self):
        reponame=self.createNewRepoAndAssertValid()
        tag="tag_"+reponame
        self.repoclient().tagRepo(reponame,tag)
        msg=self.repoclient().doHttpDelete(Constants.HTTP_PATH_STATIC+"/"+reponame+"/tags/asladalkdsla")
        self.assertEquals(msg.status, httplib.NOT_FOUND)


    def test_tag_twice_writes_tags(self):
        reponame=self.createNewRepoAndAssertValid()
        tag1="tag1_"+reponame
        tag2="tag2_"+reponame
        self.repoclient().tagRepo(reponame,tag1)
        self.repoclient().tagRepo(reponame,tag2)
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

