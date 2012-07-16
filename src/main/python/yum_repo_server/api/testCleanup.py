import unittest
import os
import shutil
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.test.testconstants import Constants
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from yum_repo_server.daemon.scheduler import MetaDataGenerationScheduler


class TestCleanup(BaseIntegrationTestCase):
    
    config = RepoConfigService()
    
    def test_cleanup_cache_dir(self):
        reponame1 = self.createStaticRepoWithContent()
        reponame2 = self.createStaticRepoWithContent()
        reponame3 = self.createStaticRepoWithContent()
        
        # delete repo 1
        shutil.rmtree(self.config.getStaticRepoDir(reponame1))
        # lock repo 2
        open(self.config.getRepoLockFile(reponame2), 'w').close()
        
        MetaDataGenerationScheduler().cleanupCacheDir()
        
        self.assertFalse(os.path.exists(self.config.getRepoCacheDir(reponame1)))
        self.assertTrue(os.path.exists(self.config.getRepoCacheDir(reponame2)))
        self.assertFalse(os.path.exists(self.config.getRepoCacheDir(reponame3)))
        
    def createStaticRepoWithContent(self):
        reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(reponame, testRPMFilePath)
        self.generate_metadata(reponame)
        repoCacheDir = self.config.getRepoCacheDir(reponame)
        self.assertTrue(os.path.exists(repoCacheDir))
        return reponame
        
if __name__ == '__main__':
    unittest.main()