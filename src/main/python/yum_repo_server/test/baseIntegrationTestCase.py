import httplib
import time
import os, shutil
import yum_repo_server
from yum_repo_server.test.liveserver import LiveServerTestCase
from yum_repo_server.test.testconstants import Constants
from yum_repo_client.repoclient import HttpClient
import pycurl



class BaseIntegrationTestCase(LiveServerTestCase):

    longMessage = True

    #remove all directories starting with TESTREPO_PREFIX in target/static/
    def setUp(self):
        projectDir = self.determineAbsolutePathToProject()
        self.cleanupDirectory(projectDir + Constants.PATH_TO_STATIC_REPO_RELATIVE_TO_PROJECT_DIR)
        self.cleanupDirectory(projectDir + Constants.PATH_TO_VIRTUAL_REPO_RELATIVE_TO_PROJECT_DIR)

    def upload_testfile(self, repo_name, path_to_file):
        self.repoclient().uploadRpm(repo_name, path_to_file)

    def determineAbsolutePathToProject(self):
        return yum_repo_server.settings.REPO_CONFIG.get('REPO_DIR')

    def createNewRepoAndAssertValid(self):
        reponame = self.uniqueRepoName()
        response = self.repoclient().createStaticRepo(reponame)
        msg = response.read()
        self.assertCreaterepoReplyValid(msg, reponame)
        return reponame

    def create_virtual_repo(self, virtual_reponame, destination_reponame):
        return self.repoclient().createVirtualRepo(virtual_reponame, destination_reponame)

    def create_virtual_repo_from_static_repo(self, virtual_reponame, static_reponame):
        return self.repoclient().createLinkToStaticRepo(virtual_reponame, static_reponame)

    def create_virtual_repo_from_virtual_repo(self, virtual_reponame, destination_reponame):
        return self.repoclient().createLinkToVirtualRepo(virtual_reponame, destination_reponame)

    def repoclient(self):
        return HttpClient(self.live_server_host, self.live_server_port)

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
            pathToMetadataFile = metadatapath + metadataFile
            self.assertTrue(os.path.exists(pathToMetadataFile),
                "File " + pathToMetadataFile + " must exist (repo metadata)")


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


    def uniqueRepoName(self):
        tstamp = int(time.time() * 1000)
        uniquereponame = Constants.TESTREPO_PREFIX + tstamp.__str__()
        return uniquereponame

    def generate_metadata(self, reponame):
        self.repoclient().generateMetadata(reponame)


    def doHttpGet(self, extPath):
        try:
            httpServ = httplib.HTTPConnection(self.live_server_host, self.live_server_port)
            httpServ.request('GET', extPath)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.live_server_host
            exit


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
        
    #### utility methods ####
    def cleanupDirectory(self, cleanupPath):
        if os.path.exists(cleanupPath):
            dirList = os.listdir(cleanupPath)
            testprefix = Constants.TESTREPO_PREFIX

            for fname in dirList:
                fpath = cleanupPath + fname
                if os.path.exists(fpath):
                    if testprefix in fname:
                        shutil.rmtree(fpath)
