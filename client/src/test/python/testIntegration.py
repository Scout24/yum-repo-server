import subprocess
import sys
import unittest
import time
import os

class init:
    
    RPM = "test5-1-1.noarch.rpm"
    RPM_WITH_PATH = "../resources/%s" % RPM 
    REPO_CLIENT = "/usr/bin/repoclient"
    YUM_HOST = "devgfs01.dev.is24.loc"
    TEST_REPO_NAME_1 = "yum-integration-test-static-repo-1"
    TEST_REPO_NAME_2 = "yum-integration-test-static-repo-2"
    TEST_VIRTUAL_NAME_1 = "yum-integration-test-virtual-repo-1"
    TEST_VIRTUAL_NAME_2 = "yum-integration-test-virtual-repo-2"

    if len(sys.argv)>1:
        YUM_HOST = str(sys.argv[1])
        print "Use yum server : %s" % YUM_HOST  
    if len(sys.argv)>2:
        REPO_CLIENT = str(sys.argv[2])
        print "Use repoclient : %s" % REPO_CLIENT  

    def repoclientExists(self):
        if not os.path.exists(self.REPO_CLIENT):
            print "\nAborting: Repoclient not found in %s!\n" % self.REPO_CLIENT
            sys.exit(1)
    
class c0_repoClient_createRepoTest(unittest.TestCase):
    
    def setUp(self):
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_1, init.YUM_HOST)
        subprocess.Popen(command, shell=True, stdout=open(os.devnull, 'wb')).wait()
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_2, init.YUM_HOST) 
        subprocess.Popen(command, shell=True, stdout=open(os.devnull, 'wb')).wait()
        
    def testCreateEmptyRepo1(self):                                                                                 
         command = "%s create %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_1, init.YUM_HOST)                   
         print "Run test : '%s'" % command                                                                           
         self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)          
     
    def testCreateEmptyRepo2(self):                                                                                 
         command = "%s create %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_2, init.YUM_HOST)                   
         print "Run test : '%s'" % command                                                                           
         self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)          
        
class c1_repoClient_uploadRPMTest(unittest.TestCase):

    def testUploadRPM(self):    
        command = "%s uploadto -s %s %s %s" % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_1, init.RPM_WITH_PATH)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testGenerateMetadata1(self):    
        command = "%s generatemetadata -s %s %s" % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_1)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)
        
                                                                                                                
    def testInListQuerystatic(self):                                                                      
              command = "%s querystatic -s %s" % (init.REPO_CLIENT, init.YUM_HOST)                              
              print "Run test : '%s'" % command                                                                 
              mysub = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)                             
              mystdout = mysub.stdout.read()                                                                    
              self.assertNotEqual(mystdout.find(init.TEST_REPO_NAME_1), -1, "command %s failed!" % command)   

class  c2_repoClient_propagateRPMTest(unittest.TestCase):
    
    def testPropagateRpmTo2(self):
        command = "%s propagate %s noarch/%s %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_1, init.RPM, init.TEST_REPO_NAME_2, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command) 
    
    def testGenerateMetadata2(self):    
        command = "%s generatemetadata -s %s %s" % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_2)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)  

        
class  c3_repoClient_propagateRepoTest(unittest.TestCase):

    def testPropagateRepo2ToRepo1(self):
        command = "%s propagaterepo %s %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_2, init.TEST_REPO_NAME_1, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)         

class  c5_repoClient_virtualTest(unittest.TestCase):
            
    def testCreateVirtualRepo1(self):
        command = "%s linktostatic %s %s -s %s" % (init.REPO_CLIENT, init.TEST_VIRTUAL_NAME_1, init.TEST_REPO_NAME_1, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)    

    def testCreateVirtualRepo2(self):
        command = "%s linktostatic %s %s -s %s" % (init.REPO_CLIENT, init.TEST_VIRTUAL_NAME_2, init.TEST_REPO_NAME_2, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command) 

    def testLinkToVirtualRepo(self):
        command = "%s linktovirtual %s %s -s %s" % (init.REPO_CLIENT, init.TEST_VIRTUAL_NAME_1, init.TEST_VIRTUAL_NAME_2, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command) 
    
    def testQueryInListVirtual(self):                                                                    
             command = "%s queryvirtual -s %s" % (init.REPO_CLIENT, init.YUM_HOST)                            
             print "Run test : '%s'" % command                                                                
             mysub = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)                            
             mystdout = mysub.stdout.read()                                                                   
             self.assertNotEqual(mystdout.find(init.TEST_VIRTUAL_NAME_1), -1, "command %s failed!" % command) 
        
    def testdeleteVirtualRepo1(self):
        command = "%s deletevirtual %s -s %s" % (init.REPO_CLIENT, init.TEST_VIRTUAL_NAME_1, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)
         
    def testdeleteVirtualRepo2(self):
        command = "%s deletevirtual %s -s %s" % (init.REPO_CLIENT, init.TEST_VIRTUAL_NAME_2, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)   


class  c6_repoClient_removeRpmAndRepoTest(unittest.TestCase):
 
    def testRemoveRPM(self):
        command = "%s deleterpm -s %s %s noarch/%s " % (init.REPO_CLIENT, init.YUM_HOST, init.TEST_REPO_NAME_1, init.RPM)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testRemoveRepo1(self):
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_1, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)

    def testRemoveRepo2(self):
        command = "%s deletestatic %s -s %s" % (init.REPO_CLIENT, init.TEST_REPO_NAME_2, init.YUM_HOST)
        print "Run test : '%s'" % command    
        self.assertEquals(subprocess.Popen(command, shell=True).wait(), 0, "command %s failed!" % command)
        

class  c7_repoClient_queryNegativeTest(unittest.TestCase):                                                  
    def testNotInListQuerystatic(self):                                                                      
        command = "%s querystatic -s %s" % (init.REPO_CLIENT, init.YUM_HOST)                                 
        print "Run test : '%s'" % command                                                            
        mysub = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)                                
        mystdout = mysub.stdout.read()                                                                       
        self.assertEqual(mystdout.find(init.TEST_REPO_NAME_1), -1, "command %s failed!" % command)             
                                                                                                             
    def testQueryNotInListVirtual(self):                                                                     
        command = "%s queryvirtual -s %s" % (init.REPO_CLIENT, init.YUM_HOST)                                 
        print "Run test : '%s'" % command                                                                    
        mysub = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)                                
        mystdout = mysub.stdout.read()                                                                       
        self.assertEqual(mystdout.find(init.TEST_VIRTUAL_NAME_1), -1, "command %s failed!" % command)          
        
        
        
if __name__ == '__main__':
    check = init()
    check.repoclientExists()
    sys.argv=[sys.argv[0]]
    unittest.main()