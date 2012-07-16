import httplib
import unittest
import os
import yum_repo_server
from  yum_repo_server.api import config
import gzip
from yum_repo_server.test.liveserver import LiveServerTestCase
from yum_repo_server.test.testconstants import Constants
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from lxml import etree

class TestCreateRepo(BaseIntegrationTestCase):

    
    def test_if_fails_if_url_is_not_correct(self):
        http_path_create_repo = '/repo/static/repo3/repodata/schmarn'
        post_data = 'name=test-repo&destination=/etc/httpd'
        response = self.doHttpPost(http_path_create_repo,post_data)
        self.assertStatusCode(response, httplib.BAD_REQUEST)

    def test_repodata_dir_was_created(self):
        repo_name = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(repo_name, testRPMFilePath)
        self.generate_metadata(repo_name)
        self.assertIsRepoMetadataGenerated(repo_name)
        self.assert_metadata_generated(repo_name)


    def assert_metadata_generated(self, repo_name):
        repo_dir = config.get_repo_dir()
        metadata_dir = repo_dir + Constants.PATH_TO_STATIC_REPO_RELATIVE_TO_PROJECT_DIR + repo_name + Constants.GENERATE_REPO_DATA_POSTFIX
        self.assertTrue(os.path.exists(metadata_dir))
        self.assertTrue(os.path.exists(metadata_dir + Constants.PRIMARY_XML))
        self.assert_package_count(metadata_dir, 1)

    def assert_package_count(self, metadata_dir, count):
        f = gzip.open(metadata_dir + Constants.PRIMARY_XML)
        try:
            tree = etree.parse(f)
            xpath_result = tree.xpath('/t:metadata/@packages', namespaces={'t' : 'http://linux.duke.edu/metadata/common'})
            package_count = xpath_result[0]
            self.assertEquals(int(package_count), count)
        finally:
            f.close()


if __name__ == '__main__':
    unittest.main()