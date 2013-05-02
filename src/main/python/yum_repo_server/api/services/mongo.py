import httplib
import logging
import pycurl
from django.http import HttpResponseRedirect
from yum_repo_server.api import config

class MongoUpdater():
    USER_AGENT = 'Yum-Repo-Server 1.0'
    log = logging.getLogger()

    def __init__(self):
        self._enabled = config.is_mongo_update_enabled()
        self._host = config.get_mongo_update_host()
        self._prefix = config.get_mongo_update_prefix()

    def delete(self, reponame, arch, rpm):
        if self._enabled:
            path = '/'.join([self._prefix, reponame, arch, rpm])
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('DELETE', path)
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated delete request %s and got response %d" % (path, response.status))

            if response.status != 204 and response.status != 404:
                self.log.error("Could not propagate delete rpm. Got response code %d" % response.status)
                raise Exception("Could not delete file %s on %s. Got response: %d" % (self._host, path, response.status))

    def propagate_rpm(self, source_repository, arch, file_name, destination_repository):
        if self._enabled:
            headers = self._create_headers({'Content-Type': 'application/x-www-form-urlencoded'})
            source_path = '/'.join([source_repository, arch, file_name])
            postdata = 'source=%s&destination=%s' % (source_path, destination_repository)
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', '/propagation', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated RPM propagation request from %s to %s and got response %d" % (source_path, destination_repository, response.status))

            if response.status != 201 and response.status != 404:
                self.log.error("Could not propagate rpm. Got response code %d" % response.status)
                raise Exception("Could not propagate rpm")

    def propagate_repository(self, source_repository, destination_repository):
        if self._enabled:
            headers = self._create_headers({'Content-Type': 'application/x-www-form-urlencoded'})
            postdata = 'source=%s&destination=%s' % (source_repository, destination_repository)
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', '/repo-propagation', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated repository propagation request from %s to %s and got response %d" % (source_repository, destination_repository, response.status))

            if response.status != 201 and response.status != 404:
                self.log.error("Could not propagate repository. Got response code %d" % response.status)
                raise Exception("Could not propagate repository")

    def uploadRpm(self, reponame, rpmPath):
        if self._enabled:
            c = pycurl.Curl()
            c.setopt(c.POST, 1)
            url = "http://%s%s/%s/" % (self._host, self._prefix, reponame)
            c.setopt(c.URL, str(url))
            c.setopt(c.HTTPPOST, [("rpmFile", (c.FORM_FILE, str(rpmPath)))])
            c.setopt(pycurl.HTTPHEADER, ['User-Agent: %s' % self.USER_AGENT])
            c.setopt(c.TIMEOUT, 300)
            try:
                c.perform()
            except Exception, e:
                self.log.error("Upload to %s failed with Exception: %s" % (url,str(e)))

            returncode = c.getinfo(pycurl.HTTP_CODE)
            c.close()

            self.log.info("Uploaded %s to %s" % (rpmPath, url))

            if returncode != httplib.CREATED:
                self.log.error("Upload to %s failed with return code %d" % (url, returncode))
                raise Exception("Upload to %s failed with return code %d" % (url, returncode))
            else:
                self.log.info("Uploaded %s to %s" % (rpmPath, url))

    def deleteRepository(self, reponame):
        if self._enabled:
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('DELETE', self._create_repo_url(reponame), None, self._create_headers())
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated repository delete request for repository %s and got response %d" % (reponame, response.status))

            if response.status != 204 and response.status != 404:
                self.log.error("Could not propagate command 'delete repository'. Got response code %d" % response.status)
                raise Exception("Could not propagate command 'delete repository'")

    def createRepository(self, reponame):
         if self._enabled:
            headers = self._create_headers({'Content-Type': 'application/x-www-form-urlencoded'})
            postdata = "name=%s" % reponame
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST','/repo/', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close()
                          
            self.log.info("Propagated create repository %s" % reponame)

            if response.status != 201 and response.status != 404:
                self.log.error("Could not propagate command 'create repository'. Got response code %d" % response.status)
                raise Exception("Could not propagate command 'create repository'. Got response code %d with message: %s",
                                response.status, response.read())                   

    def redirect(self, fullpath):
        segments = fullpath.split('/')
        return HttpResponseRedirect('http://' + self._host + self._prefix + '/' + '/'.join(segments[-3:]))

    def generate_meta_data(self, reponame):
        if self._enabled:
            response = self._request_response('POST', self._create_repo_url(reponame, 'repodata'),
                                              self._create_headers())

            self.log.info("Propagated generate meta data request for repository %s and got response %d" % (
                reponame, response.status))

            if response.status != 201:
                self.log.error("Could not propagate command 'generatemetadata'. Got response code %d" % response.status)
                raise Exception("Could not propagate command 'generatemetadata'")

    def delete_virtual_repo(self, reponame):
        if self._enabled:
            response = self._request_response('DELETE', self._create_repo_url('virtual', reponame),
                                              self._create_headers())

            self.log.info("Propagate delete virtual repo %s and got response %d",
                          reponame, response.status)

            if response.status != 204:
                self.log.error("Could not propagate command 'delete virtual repo'. Got response code %d" % response.status)
                raise Exception("Could not propagate command 'delete virtual repo'. Got reponse code %d with message: %s",
                                response.status, response.read())

    def create_virtual_repo(self, virtual_reponame, destination):
        if self._enabled:
            headers = self._create_headers({'Content-Type': 'application/x-www-form-urlencoded'})
            postdata = 'name=%s&destination=%s' % (virtual_reponame, destination)
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', self._create_repo_url('virtual'), postdata, headers)
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated create virtual repository %s linked to %s and got response %d" % (virtual_reponame, destination, response.status))

            if response.status != 201 and response.status != 404:
                self.log.error("Could not propagate command 'create virtual repo'. Got response code %d" % response.status)
                raise Exception("Could not propagate command 'create virtual repo'. Got response code %d with message: %s",
                                response.status, response.read())

    def _create_repo_url(self, reponame, action=''):
        return '/'.join([self._prefix, reponame, action])

    def _create_headers(self, additional_headers=None):
        headers = {'User-Agent': self.USER_AGENT}
        if additional_headers:
            headers.update(additional_headers)

        return headers

    def _request_response(self, method, url, headers=None):
        http_connection =  httplib.HTTPConnection(self._host)
        try:
            http_connection.connect()
            http_connection.request(method, url, None, headers)
            return http_connection.getresponse()
        finally:
            if http_connection:
                http_connection.close()
