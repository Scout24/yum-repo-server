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
                raise Exception("Could not propagate")

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
                raise Exception("Could not propagate")

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
                raise Exception("Could not propagate")

    def redirect(self, fullpath):
        segments = fullpath.split('/')
        return HttpResponseRedirect('http://' + self._host + self._prefix + '/' + '/'.join(segments[-3:]))

    def generate_meta_data(self, reponame):
        if self._enabled:
            response = self._request_response('POST', self._create_repo_url(reponame, 'repodata'), self._create_headers())

            self.log.info("Propagated generate meta data request for repository %s and got response %d" % (
                reponame, response.status))

            if response.status != 201:
                raise Exception("Could not propagate command 'generatemetadata'")

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
