import httplib

from yum_repo_client.repoclient import RepoException
from yum_repo_server.test import Constants, unique_repo_name
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from yum_repo_client.repoclient import CommandLineClient
import sys
import os

class TestRepoClient(BaseIntegrationTestCase):
    
    def test_create_repo(self):
        reponame = unique_repo_name();
        self._execute(['create', reponame]);
        response = self.helper.do_http_get('/repo/' + reponame + '/')
        self.assertStatusCode(response, httplib.OK)
        
    def test_delete_static_repo(self):
        reponame = unique_repo_name();
        self._execute(['create', reponame]);
        self._execute(['deletestatic', reponame])
        
        response = self.helper.do_http_get('/repo/' + reponame + '/')
        self.assertStatusCode(response, httplib.NOT_FOUND)
        
    def test_upload_rpm(self):
        reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self._execute(['uploadto', reponame, testRPMFilePath])
        response = self.helper.do_http_get('/repo/' + reponame + '/' + Constants.TEST_RPM_DESTINATION_NAME)
        self.assertStatusCode(response, httplib.OK)
        
    def test_filter_static_reponames_with_regex(self):
        self.createNewRepoAndAssertValid()
        self.repoclient().createStaticRepo('testrepo-123.1.42')
        realout = sys.stdout
        logfile_name = 'test-log-filter_static_reponames_with_regex.log'
        sys.stdout = open(logfile_name, 'w')
        self._execute(['querystatic', '-name', 'testrepo-[\d\.]+'])
        sys.stdout.close()
        sys.stdout = realout
        
        logfile = open(logfile_name, 'r')
        test_output = filter(lambda entry: entry.strip() != '',logfile.readlines())
        logfile.close()
        os.remove(logfile_name)
        
        self.assertEquals(1, len(test_output))
        self.assertEquals('testrepo-123.1.42\n', test_output[0])
        
    def test_filter_virtual_reponames_with_regex(self):
        reponame = self.createNewRepoAndAssertValid()
        self.repoclient().createLinkToStaticRepo('testrepo1-123', reponame)
        self.repoclient().createLinkToStaticRepo('testrepo-123.1.42', reponame)
        realout = sys.stdout
        logfile_name = 'test-log-filter_virtual_reponame_with_regex.log'
        sys.stdout = open(logfile_name, 'w')
        self._execute(['queryvirtual', '-name', 'testrepo-[\d\.]+'])
        sys.stdout.close()
        sys.stdout = realout
        
        logfile = open(logfile_name, 'r')
        test_output = filter(lambda entry: entry.strip() != '',logfile.readlines())
        logfile.close()
        os.remove(logfile_name)
        
        self.assertEquals(1, len(test_output))
        self.assertEquals('testrepo-123.1.42\n', test_output[0])
        
    def test_delete_rpm(self):
        reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(reponame, testRPMFilePath)
        self._execute(['deleterpm', reponame, Constants.TEST_RPM_DESTINATION_NAME]);
        response = self.helper.do_http_get('/repo/' + reponame + '/' + Constants.TEST_RPM_DESTINATION_NAME)
        self.assertStatusCode(response, httplib.NOT_FOUND)
        
    def test_generatemetadata(self):
        reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(reponame, testRPMFilePath)
        self._execute(['generatemetadata', reponame]);
        response = self.helper.do_http_get('/repo/' + reponame + '/repodata/repomd.xml')
        self.assertStatusCode(response, httplib.OK)
        
    def test_linktostatic(self):
        static_reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(static_reponame, testRPMFilePath)
        virtual_reponame = unique_repo_name()
        self._execute(['linktostatic', virtual_reponame, static_reponame])
        response = self.helper.do_http_get('/repo/virtual/' + virtual_reponame + '/' + Constants.TEST_RPM_DESTINATION_NAME)
        self.assertStatusCode(response, httplib.OK)
        
    def test_linktovirtual(self):
        static_reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(static_reponame, testRPMFilePath)
        virtual_reponame = unique_repo_name()
        self.create_virtual_repo_from_static_repo(virtual_reponame, static_reponame)
        virtual_reponame2 = unique_repo_name()
        self._execute(['linktovirtual', virtual_reponame2, virtual_reponame])
        response = self.helper.do_http_get('/repo/virtual/' + virtual_reponame2 + '/' + Constants.TEST_RPM_DESTINATION_NAME)
        self.assertStatusCode(response, httplib.OK)
        
    def test_deletevirtual(self):
        static_reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_RPM_FILE_NAME
        self.upload_testfile(static_reponame, testRPMFilePath)
        virtual_reponame = unique_repo_name()
        self.create_virtual_repo_from_static_repo(virtual_reponame, static_reponame)
        self._execute(['deletevirtual', virtual_reponame])
        response = self.helper.do_http_get('/repo/virtual/' + virtual_reponame + '/')
        self.assertStatusCode(response, httplib.NOT_FOUND)
        
    def test_upload_of_invalid_rpm(self):
        reponame = self.createNewRepoAndAssertValid()
        testRPMFilePath = Constants.TEST_RPM_FILE_LOC + Constants.TEST_CORRUPT_RPM
        self.assertEquals(1, self._execute(['uploadto', reponame, testRPMFilePath]))    
    
    def _execute(self, arguments):
        return CommandLineClient(self._baseargs() + arguments).execute()
        
    def _baseargs(self):
        return ['', '--hostname=' + self.live_server_host, '--port=' + self.live_server_port.__str__()]