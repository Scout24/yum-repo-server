import httplib
import unittest
import os
import yaml
from yum_repo_client.repoclient import RepoException
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.test.testconstants import Constants
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase


class TestVirtualRepo(BaseIntegrationTestCase):

    def test_if_destination_path_not_exists(self):
        self.assertRaises(RepoException, self.create_virtual_repo, 'test-repo', '/etc/target_does_not_exist')

    def test_if_name_param_not_exists(self):
        post_data = 'destination=static/target_does_not_exist'
        response = self.doHttpPost(Constants.HTTP_PATH_VIRTUAL, post_data)
        self.assertEquals(response.status, httplib.BAD_REQUEST)

    def test_if_destination_param_not_exists(self):
        post_data = 'name=test-repo'
        response = self.doHttpPost(Constants.HTTP_PATH_VIRTUAL, post_data)
        self.assertEquals(response.status, httplib.BAD_REQUEST)

    def test_create_link_to_static_repo(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        self.assertVirtualRepoConfig(virtual_reponame, static_reponame)

    def test_create_link_to_virtual_repo(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        new_virtual_reponame = "alias_to_" + virtual_reponame
        #now alias the aliased repo!
        response = self.create_virtual_repo_from_virtual_repo(new_virtual_reponame, virtual_reponame)
        self.assertEquals(response.status, httplib.CREATED)
        self.assertVirtualRepoConfig(new_virtual_reponame, static_reponame)

    def test_create_double_link(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        new_virtual_reponame = "alias_to_" + virtual_reponame
        #now alias the aliased repo!
        response = self.create_virtual_repo_from_virtual_repo(new_virtual_reponame, virtual_reponame)
        self.assertEquals(response.status, httplib.CREATED)
        self.assertVirtualRepoConfig(virtual_reponame, static_reponame)

        #rePOST exactly the same thing
        response = self.create_virtual_repo_from_virtual_repo(new_virtual_reponame, virtual_reponame)
        content = response.read()
        self.assertTrue('"link_already_existed": true' in content,
            "alias should detect when same link already existed")


    def test_is_virtual_repository_path(self):
        virtual_path_example = "target/virtual/foobarrepo"
        static_path_example = "target/static/foobarrepo"
        self.assertTrue(RepoConfigService().is_virtual_repository_path(virtual_path_example))
        self.assertFalse(RepoConfigService().is_virtual_repository_path(static_path_example))


    def test_deliver_rpm_file_from_virtual_repository(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(static_reponame, testRPMFilePath)
        self.generate_metadata(static_reponame)
        self.assertRpmDownloadable(virtual_reponame)
        
    def test_serve_directory_listing(self):
        self.assertCreateVirtualRepo()
        response = self.helper.do_http_get('/repo/virtual/')
        self.assertEqual(response.status, httplib.OK)
        
    def test_should_not_delete_files_in_virtual_repos(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        response = self.repoclient().doHttpDelete('/repo/virtual/' + virtual_reponame + '/fail')
        self.assertEqual(response.status, httplib.NOT_IMPLEMENTED)
        
    def test_delete_non_existing_repo(self):
        try:
            response = self.repoclient().deleteVirtualRepo('just-a-dummy-repo')
            raise AssertionError('expected an error but got status: '+response.status)
        except RepoException:
            return True

    def test_delete_virtual_repo(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        response = self.repoclient().deleteVirtualRepo(virtual_reponame)
        self.assertEqual(response.status, httplib.NO_CONTENT)
        response = self.helper.do_http_get('/repo/virtual/' + virtual_reponame)
        self.assertEqual(response.status, httplib.NOT_FOUND)
        
    def test_should_provide_repo_info(self):
        static_reponame, virtual_reponame = self.assertCreateVirtualRepo()
        response = self.helper.do_http_get('/repo/virtual/' + virtual_reponame + '.json')
        self.assertEqual(response.status, httplib.OK)
        content = response.read()
        self.assertEquals(content, '{"destination": "/static/' + static_reponame + '"}')

    def assertCreateVirtualRepo(self):
        static_repo_name = self.createNewRepoAndAssertValid()
        self.generate_metadata(static_repo_name)
        virtual_reponame = Constants.TESTREPO_PREFIX + static_repo_name
        response = self.create_virtual_repo_from_static_repo(virtual_reponame, static_repo_name)
        self.assertEquals(response.status, httplib.CREATED)
        return static_repo_name, virtual_reponame

    def assertVirtualRepoConfig(self, virtual_reponame, static_reponame):
        metadatapath = RepoConfigService().getVirtualRepoDir(virtual_reponame) + "/" + RepoConfigService.ALIAS_METADATA_FILENAME
        self.assertTrue(os.path.exists(metadatapath), "No metadata generated -> alias failed")
        alias_file = open(metadatapath)
        alias_config = yaml.load(alias_file)
        alias_file.close()
        alias_metadata = dict(alias_config.items())
        config_destination = alias_metadata['destination']
        expected_destination = "/static/" + static_reponame
        self.assertEquals(config_destination, expected_destination, "Alias points to wrong repo")

    def assertRpmDownloadable(self, virtual_reponame):
        rpmURL = Constants.HTTP_PATH_VIRTUAL + '/' + virtual_reponame + '/' + Constants.TEST_RPM_DESTINATION_NAME
        response = self.helper.do_http_get(rpmURL)
        self.assertEquals(response.status, httplib.OK)
        
if __name__ == '__main__':
    unittest.main()

