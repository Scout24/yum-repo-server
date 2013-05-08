import httplib
from dummy_thread import exit
import pycurl
import string
import base64
import urllib


class RepoException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)


class HttpClient(object):
    USER_AGENT = 'repoclient/1.0'

    username = None
    password = None
    message = None

    def __init__(self, hostname, port, message=None):
        self.hostname = hostname
        self.port = port
        self.message = message

    def queryStatic(self, params):
        urlparams = urllib.urlencode(params)
        response = self.doHttpGet('/repo.txt?%s' % urlparams)
        self.assertResponse(response, httplib.OK)
        return response

    def queryVirtual(self, params):
        urlparams = urllib.urlencode(params)
        response = self.doHttpGet('/repo/virtual.txt?' + urlparams)
        self.assertResponse(response, httplib.OK)
        return response

    def createStaticRepo(self, reponame):
        response = self.doHttpPost('/repo/', "name=" + reponame)
        self.assertResponse(response, httplib.CREATED)
        return response

    def delete_static_repo(self, reponame):
        response = self.doHttpDelete('/repo/%s' % reponame)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def untagRepo(self, reponame, tag):
        response = self.doHttpDelete('/repo/' + reponame + '/tags/' + tag)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def tagRepo(self, reponame, tag):
        response = self.doHttpPost('/repo/' + reponame + '/tags/', "tag=" + tag)
        self.assertResponse(response, httplib.CREATED)
        return response

    def tagList(self, reponame):
        response = self.doHttpGet('/repo/' + reponame + '/tags/')
        self.assertResponse(response, httplib.OK)
        return response

    def uploadRpm(self, reponame, rpm_file_name):
        c = pycurl.Curl()
        c.setopt(c.POST, 1)
        url = "http://%s:%d/repo/%s/" % (self.hostname, self.port, reponame)
        c.setopt(c.URL, url)
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

    def propagate_rpm(self, fromrepo, rpm_arch_slash_name, torepo):
        post_data = 'source=' + fromrepo + "/" + rpm_arch_slash_name + "&destination=" + torepo
        response = self.doHttpPost('/propagation/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def propagate_repo(self, source_repository, destination_repository):
        post_data = 'source=' + source_repository + "&destination=" + destination_repository
        response = self.doHttpPost('/repo-propagation/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def get_archs(self, reponame):
        response = self.doHttpGet('/repo/' + reponame + '.json')
        self.assertResponse(response, httplib.OK)
        return response

    def get_files(self, reponame, arch):
        response = self.doHttpGet('/repo/' + reponame + '/' + arch + '.json')
        self.assertResponse(response, httplib.OK)
        return response

    def doHttpPost(self, extPath, postdata='', headers=None):
        if postdata and self.message:
            postdata += '&YRS_MESSAGE=' + str(self.message)
        if not headers:
            headers = {}
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
            exit()

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
            exit()

    def doHttpGet(self, extPath):
        headers = {'User-Agent': self.USER_AGENT}

        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth

        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.request('GET', extPath, None, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit()

    def assertResponse(self, response, expectedStatus):
        if response.status != expectedStatus:
            raise RepoException(
                "ERROR: Got unexpected status code %(status)d .Expected %(expected)d Response: %(content)s" % {
                    "status": response.status,
                    "expected": expectedStatus,
                    "content": response.read()})
