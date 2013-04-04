import httplib
import os, shutil
import yum_repo_server
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.test.liveserver import LiveServerTestCase
from yum_repo_server.test import Constants, unique_repo_name
from yum_repo_client.repoclient import HttpClient
from yum_repo_server.test.integrationTestHelper import IntegrationTestHelper



class BaseIntegrationTestCase(LiveServerTestCase):

    longMessage = True

    #remove all directories starting with TESTREPO_PREFIX in target/static/
    def setUp(self):
        projectDir = self.determineAbsolutePathToProject()
        self.cleanupDirectory(projectDir + Constants.PATH_TO_STATIC_REPO_RELATIVE_TO_PROJECT_DIR)
        self.cleanupDirectory(projectDir + Constants.PATH_TO_VIRTUAL_REPO_RELATIVE_TO_PROJECT_DIR)
        self.helper = IntegrationTestHelper(self.live_server_host, self.live_server_port)

    def upload_testfile(self, repo_name, path_to_file):
        self.repoclient().uploadRpm(repo_name, path_to_file)

    def determineAbsolutePathToProject(self):
        return yum_repo_server.settings.REPO_CONFIG.get('REPO_DIR')

    def createNewRepoAndAssertValid(self):
        reponame = unique_repo_name()
        response = self.repoclient().createStaticRepo(reponame)
        msg = response.read()
        self.assertCreaterepoReplyValid(msg, reponame)
        return reponame
    
    def assert_create_virtual_repo(self):
        static_repo_name = self.createNewRepoAndAssertValid()
        self.generate_metadata(static_repo_name)
        virtual_reponame = Constants.TESTREPO_PREFIX + static_repo_name
        response = self.create_virtual_repo_from_static_repo(virtual_reponame, static_repo_name)
        self.assertEquals(response.status, httplib.CREATED)
        return static_repo_name, virtual_reponame

    def assert_repository_contains(self, repository, architecture, file_name):
        repository_contains_file = self._repository_contains(architecture, file_name, repository)
        self.assertTrue(repository_contains_file, "Repository {0} does not contain file {2}/{1}".format(repository, file_name, architecture))

    def assert_repository_does_not_contain(self, repository, architecture, file_name):
        repository_contains_file = self._repository_contains(architecture, file_name, repository)
        self.assertFalse(repository_contains_file, "Repository {0} contains file {2}/{1}".format(repository, file_name, architecture))

    def create_virtual_repo(self, virtual_reponame, destination_reponame):
        return self.repoclient().createVirtualRepo(virtual_reponame, destination_reponame)

    def create_virtual_repo_from_static_repo(self, virtual_reponame, static_reponame):
        return self.repoclient().createLinkToStaticRepo(virtual_reponame, static_reponame)

    def create_virtual_repo_from_virtual_repo(self, virtual_reponame, destination_reponame):
        return self.repoclient().createLinkToVirtualRepo(virtual_reponame, destination_reponame)

    def repoclient(self):
        return HttpClient(self.live_server_host, self.live_server_port,"Integration Test")

    def determine_repo_path(self, reponame):
        projectDir = self.determineAbsolutePathToProject()
        repoRootRelativeToProjectDir = Constants.PATH_TO_STATIC_REPO_RELATIVE_TO_PROJECT_DIR + reponame + "/"
        repopath = projectDir + repoRootRelativeToProjectDir
        return repopath

    def assertIsRepoMetadataGenerated(self,reponame):
        repopath = self.determine_repo_path(reponame)
        metadatapath = repopath + "repodata/"
        expectedMetadataFiles = ["filelists.xml.gz", "other.xml.gz", "primary.xml.gz", "repomd.xml"]
        for metadataFile in expectedMetadataFiles:
            self.assertFilenameSuffixExistsInDir(metadatapath, metadataFile)

    def assertFilenameSuffixExistsInDir(self, path, suffix):
        for filename in os.listdir(path):
            if filename.endswith(suffix):
                return

        raise AssertionError('No file with suffix ' + suffix + ' in ' + path)

    #the repo replies with name and dir if successful
    def assertCreaterepoReplyValid(self, msg, reponame):
        self.assertTrue(self.isInString('"name": "' + reponame + '', msg), "repo name must be in createrepo reply")
        self.assertTrue(self.isInString('"dir":', msg), "repo dir must be in createrepo reply")

    def assertStatusCode(self, httpResponse, expectedCode):
        if httpResponse.status != expectedCode:
            message = 'Http response:' + httpResponse.read()
        else:
            message = None
        self.assertEquals(httpResponse.status, expectedCode, message)

    def isInString(self, substring, string):
        isSubstring = substring in string
        return isSubstring

    def generate_metadata(self, reponame):
        self.repoclient().generateMetadata(reponame)

    def doHttpPost(self, extPath, postdata='', headers = {}):
        try:
            httpServ = httplib.HTTPConnection(self.live_server_host, self.live_server_port)
            httpServ.connect()
            httpServ.request('POST', extPath, postdata, headers)
            response = httpServ.getresponse()
            httpServ.close()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.live_server_host
            exit
    def doHttpGet(self, extPath, headers = {}):
        try:
            httpServ = httplib.HTTPConnection(self.live_server_host, self.live_server_port)
            httpServ.connect()
            httpServ.request('GET', extPath, None, headers)
            response = httpServ.getresponse()
            httpServ.close()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.live_server_host
            exit
        
    #### utility methods ####
    def cleanupDirectory(self, cleanupPath):
        if os.path.exists(cleanupPath):
            dirList = os.listdir(cleanupPath)

            for fname in dirList:
                fpath = cleanupPath + fname
                if os.path.exists(fpath):
                    shutil.rmtree(fpath)

    def _repository_contains(self, architecture, file_name, repository):
        repository_path = RepoConfigService().getStaticRepoDir(repository)
        path_to_arch = os.path.join(repository_path, architecture)

        if not os.path.exists(path_to_arch):
            return False

        for file_in_dir in os.listdir(path_to_arch):
            if file_in_dir.endswith(file_name):
                return True

        return False

