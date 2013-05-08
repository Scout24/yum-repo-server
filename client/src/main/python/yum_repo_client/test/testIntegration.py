import subprocess
import sys
import unittest2
import time
import os

class init:
    RPM = "test5-1-1.noarch.rpm"
    RPM_WITH_PATH = "../resources/%s" % RPM 
    REPO_CLIENT = "/usr/bin/repoclient"
    YUM_HOST = "devgfs01.dev.is24.loc"
    TEST_REPO_NAME_DEV = "yum-integration-test-repo-dev"
    TEST_REPO_NAME_TUV = "yum-integration-test-repo-tuv"

    if sys.argv[1]:
        YUM_HOST = str(sys.argv[1])
        print "Use yum server : %s" % YUM_HOST  
    if sys.argv[2]:
        REPO_CLIENT = str(sys.argv[2])
        print "Use repoclient : %s" % REPO_CLIENT  

    def repoclientExists(self):
        if not os.path.exists(self.REPO_CLIENT):
            print "\nAborting: Repoclient not found in %s!\n" % self.REPO_CLIENT
            sys.exit()

class c1_repoclient_createRepoAndUploadRPMTest(unittest2.TestCase):

    def testCreateEmptyRepo(self):
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_DEV, init.YUM_HOST)
        subprocess.Popen(command, shell=True).wait()
        command = "%s create %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_DEV, init.YUM_HOST)    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testUploadRPM(self):    
        command = "%s uploadto -s %s %s %s" % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_DEV, init.RPM_WITH_PATH)    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testGenerateMetadata(self):    
        command = "%s generatemetadata -s %s %s" % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_DEV)    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)
        time.sleep( 5 )

class  c2_repoclient_removeRpmAndRepoTest(unittest2.TestCase):
 
    def testRemoveRPM(self):
        command = "%s deleterpm -s %s %s noarch/%s " % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_DEV, init.RPM)    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testRemoveRepo(self):
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_DEV, init.YUM_HOST)    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)


if __name__ == '__main__':
    check = init()
    check.repoclientExists()
    unittest2.main()