#!/usr/bin/env python
import os
import httplib
from dummy_thread import exit
import pycurl
import sys
import getpass
import string
import base64
import yaml
import re

class RepoException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)


class HttpClient(object):
    USER_AGENT = 'repoclient/1.0'
    
    username = None
    password = None
    
    def __init__(self, hostname, port):
        self.hostname = hostname
        self.port = port

    def createStaticRepo(self, reponame):
        response = self.doHttpPost('/repo/', "name=" + reponame)
        self.assertResponse(response, httplib.CREATED)
        return response

    def uploadRpm(self, reponame, rpm_file_name):
        c = pycurl.Curl()
        c.setopt(c.POST, 1)
        url = "http://%s:%d/repo/%s/" % (self.hostname, self.port, reponame)
        c.setopt(c.URL,url )
        c.setopt(c.HTTPPOST, [("rpmFile", (c.FORM_FILE, rpm_file_name))])
        c.setopt(pycurl.HTTPHEADER, ['User-Agent: ' + self.USER_AGENT])
        if self.username is not None:
            c.setopt(pycurl.HTTPAUTH, pycurl.HTTPAUTH_BASIC)
            c.setopt(pycurl.USERPWD, '%s:%s' % (self.username, self.password))

        c.perform()
        returncode = c.getinfo(pycurl.HTTP_CODE)
        c.close()
        
        if returncode != httplib.CREATED:
            raise RepoException("Upload failed.")

    def deleteSingleRpm(self, reponame, rpm_file_name):
        response = self.doHttpDelete('/repo/' + reponame + '/' + rpm_file_name)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def generateMetadata(self, reponame):
        response = self.doHttpPost('/repo/' + reponame + '/repodata')
        self.assertResponse(response, httplib.CREATED)
        return response
    
    def propagate_rpm(self, source, destination):
        response = self.doHttpPost('/propagation/', 'source=%s&destination=%s' % (source, destination))
        self.assertResponse(response, httplib.CREATED)
        return response
    
    def createVirtualRepo(self, virtual_reponame, destination_reponame):
        post_data = 'name=' + virtual_reponame + "&destination=" + destination_reponame
        response = self.doHttpPost('/repo/virtual/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def createLinkToVirtualRepo(self, virtual_reponame, destination_virtual_reponame):
        return self.createVirtualRepo(virtual_reponame, 'virtual/' + destination_virtual_reponame)
    
    def createLinkToStaticRepo(self, virtual_reponame, static_reponame):
        return self.createVirtualRepo(virtual_reponame, 'static/' + static_reponame)
    
    def deleteVirtualRepo(self, virtual_reponame):
        response = self.doHttpDelete('/repo/virtual/' + virtual_reponame)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def propagateRpm(self,fromrepo,rpm_arch_slash_name,torepo):
        post_data = 'source=/'+fromrepo+"/"+rpm_arch_slash_name+"&destination=/"+torepo
        response = self.doHttpPost('/propagate/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def doHttpPost(self, extPath, postdata='', headers=None):
        if not headers: headers = {}
        headers['User-Agent'] = self.USER_AGENT
        
        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth
        
        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.connect()
            httpServ.request('POST', extPath, postdata, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit
            
    def doHttpDelete(self, extPath):
        headers = {'User-Agent': self.USER_AGENT}
        
        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth
        
        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.request('DELETE', extPath, None, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit

    def assertResponse(self, response, expectedStatus):
        if response.status != expectedStatus:
            raise RepoException(
                "ERROR: Got unexpected status code %(status)d .Expected %(expected)d Response: %(content)s" % {
                    "status": response.status,
                    "expected": expectedStatus,
                    "content": response.read()})


class CommandLineClient(object):
    operations = {}
    arguments = []
    options = {}

    def __init__(self, arguments):
        self.arguments = arguments
        
        self.options = CommandLineClientOptionsExtractor()
        self.options.extract_and_remove_options_from_arguments(self.arguments)
        
        self.httpClient = HttpClient(hostname=self.options.hostname, port=self.options.port)
        self.initOperations()

    def execute(self):
        if len(self.arguments) < 2:
            return self.showHelp()

        operation = self.arguments[1]
        if not operation in self.operations:
            print "ERROR: operation " + operation + " not found."
            return self.showHelp()

        if self.options.username is not None:
            self._readPassword()
        if self.options.hostname is None:
            print "ERROR: you must specify a hostname using --hostname=<hostname>"
            return self.showHelp()

        operationMethod = self.operations[operation]
        return operationMethod(self)

    def propagateRpm(self):
        if len(self.arguments) < 4:
            print "ERROR: Please specify source, rpm and target repository."
            return self.showHelp()
        fromrepo=self.arguments[2]
        rpm_arch_slash_name=self.arguments[3]
        torepo=self.arguments[4]
        
        #quick sanity check
        pattern=re.compile('[\w]+/[\w]+')
        match=pattern.match(rpm_arch_slash_name)
        if match is None:
            print "ERROR : Your input, '"+rpm_arch_slash_name+"""' did not match the required pattern.
        It should look like this : <rpm_arch>/<rpm_name>"""
            return 1

        try:
            self.httpClient.propagateRpm(fromrepo, rpm_arch_slash_name, torepo)
            return 0
        except Exception, e:
            print e
            return 1

    def createStaticRepo(self):
        if len(self.arguments) < 3:
            print "ERROR: Please specify a reponame."
            return self.showHelp()

        reponame = self.arguments[2]
        try:
            self.httpClient.createStaticRepo(reponame)
            return 0
        except Exception, e:
            print e
            return 1

    def uploadRpms(self):
        if len(self.arguments) < 4:
            print "ERROR: Please specify rpm file(s) to upload."
            return self.showHelp()

        reponame = self.arguments[2]
        error = False
        try:
            for rpm_file_name in self.arguments[3:]:
                if not os.path.exists(rpm_file_name):
                    print 'ERROR: %s not found' % rpm_file_name
                    error = True
                    continue
                self.httpClient.uploadRpm(reponame, rpm_file_name)
                print '\t' + rpm_file_name

            if error:
                return 1
            return 0
        except Exception, e:
            print e
            return 1

    def deleteRpms(self):
        if len(self.arguments) < 4:
            print "ERROR: Please specify rpm file to remove."
            return self.showHelp()

        reponame = self.arguments[2]
        try:
            for rpm_file_name in self.arguments[3:]:
                self.httpClient.deleteSingleRpm(reponame, rpm_file_name)
                print '\t' + rpm_file_name
            return 0
        except Exception, e:
            print e
            return 1

    def generateMetadata(self):
        if len(self.arguments) < 3:
            print "ERROR: Please specify a reponame."
            return self.showHelp()

        reponame = self.arguments[2]
        try:
            self.httpClient.generateMetadata(reponame)
            return 0
        except Exception, e:
            print e
            return 1
        
    def createLinkToStatic(self):
        if len(self.arguments) < 4:
            print "ERROR: Please specify a virtual repository name and a static repository name."
            return self.showHelp()
        virtual_reponame = self.arguments[2]        
        static_reponame = self.arguments[3]
        try:
            self.httpClient.createLinkToStaticRepo(virtual_reponame, static_reponame)
            return 0
        except Exception, e:
            print e
            return 1     
        
    def createLinkToVirtual(self):
        if len(self.arguments) < 4:
            print "ERROR: Please specify two virtual repository names."
            return self.showHelp()
        virtual_reponame = self.arguments[2]        
        destination_virtual_reponame = self.arguments[3]
        try:
            self.httpClient.createLinkToVirtualRepo(virtual_reponame, destination_virtual_reponame)
            return 0
        except Exception, e:
            print e
            return 1
        
    def deleteVirtualRepo(self):
        if len(self.arguments) < 3:
            print "ERROR: Please specify a reponame."
            return self.showHelp()

        reponame = self.arguments[2]
        try:
            self.httpClient.deleteVirtualRepo(reponame)
            return 0
        except Exception, e:
            print e
            return 1    


    def showHelp(self):
        print """
    Yum Repo Server Client Command Line Tool
    
    Options:
        create <reponame>  : Creates a new empty repository on the server
        uploadto <reponame> <rpm1> ... <rpmN> : Uploads rpms to a dedicated repository on the server
        deleterpm <reponame> <rpm1> ... <rpmN> : Deletes rpms from the server
        generatemetadata <reponame> : Generates Yum Metadata for this repository
        linktostatic <virtual_reponame> <static_reponame> : Creates a virtual repository linking to a static repository
        linktovirtual <virtual_reponame> <virtual_reponame> : Creates a virtual repository linking to another virtual repository
        deletevirtual <virtual_reponame> : Deletes the virtual repository, but leaves the static repository untouched
        propagate <repo1> <arch>/<name> <repo2> : Propagates most recent matching rpm from repo1 to repo2
        
        --hostname=<hostname> : hostname of the yum repo server. Default: set by /etc/yum-repo-client.yaml 
        --port=<port> : port of the yum repo server. Default: 80 unless set by /etc/yum-repo-client.yaml
        --username=<username> : username to use basic authentication. You will be prompted for the password.
        """
        return 1

    def initOperations(self):
        self.operations = {
                'create': CommandLineClient.createStaticRepo, 
                'uploadto': CommandLineClient.uploadRpms,
                'generatemetadata' : CommandLineClient.generateMetadata,
                'linktostatic' : CommandLineClient.createLinkToStatic,
                'linktovirtual' : CommandLineClient.createLinkToVirtual,
                'deletevirtual' : CommandLineClient.deleteVirtualRepo,
                'deleterpm' : CommandLineClient.deleteRpms,
                'propagate' : CommandLineClient.propagateRpm,
        }


    def assertResponse(self, response, expectedStatus):
        if response.status != expectedStatus:
            print "ERROR: Got unexpected status code %(status)d . Response: %(content)s" % {"status": response.status,
                                                                                            "content": response.read()}
            return 1
        else:
            print "\n" + response.read() + "\n"
            return 0
        
    def _readPassword(self):
        password = getpass.getpass()
        self.httpClient.username = self.options.username
        self.httpClient.password = password


class OptionParsingException(Exception): pass

class CommandLineClientOptionsExtractor(object):
    '''
        Extracts known options (properties of this class) and removes them from the
        given argument list. Options are marked through two hyphen at the beginning
        of the argument.
    '''
    def __init__(self):
        if 'YUM_REPO_CLIENT_CONFIG' in os.environ:
            config_filename = os.environ['YUM_REPO_CLIENT_CONFIG']
        else:
            config_filename = '/etc/yum-repo-client.yaml'
            
        if os.path.exists(config_filename):
            f = open(config_filename)
            try:
                config = yaml.load(f)
                self.hostname = config['DEFAULT_HOST']
                self.port = config['DEFAULT_PORT']
            finally:
                f.close()
    
    username = None
    hostname = None
    port = None
    
    def _set_hostname(self, hostname):
        self.hostname = hostname

    def _set_port(self, port):
        try:
            self.port = int(port)
        except ValueError:
            raise OptionParsingException('Could not cast %s to an int.' % port)
        
    def _set_username(self, username):
        self.username = username
    
    _options = {
        'hostname' : _set_hostname,
        'port' : _set_port,
        'username' : _set_username,
    }
    
    def extract_and_remove_options_from_arguments(self, arguments):
        '''
            Extracts known options (properties of this class) and removes them from the
            given argument list. Options are marked through two hyphen at the beginning
            of the argument.
        '''
        originalArgs = arguments[:]
        for argument in originalArgs:
            if argument.startswith('--'):
                self._set_known_option_and_remove_it_from_argument_list(argument, arguments)

    def _set_known_option_and_remove_it_from_argument_list(self, possible_option, argument_list):
        '''
            Sets and removes only known options. The other options will be ignored.
        '''
        argumentKey, argumentValue = self._split_argument(possible_option[2:])
        
        for optionKey in self._options.keys():
            if optionKey == argumentKey:
                self._options.get(optionKey)(self, argumentValue)
                argument_list.remove(possible_option)
        
                        
    def _split_argument(self, argument):
        argumentParts = argument.split('=', 1)
        if len(argumentParts) < 2:
            raise OptionParsingException('Missing equality sign and value for %s' % argumentParts)
        
        return argumentParts[0], argumentParts[1]

def mainMethod():
    exitCode = CommandLineClient(sys.argv).execute()
    sys.exit(exitCode) 

if __name__ == '__main__':
    mainMethod()
    
