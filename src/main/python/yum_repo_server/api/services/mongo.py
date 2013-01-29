import httplib
import logging
import pycurl
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
            headers = {
                'User-Agent': self.USER_AGENT,
                }
            path = '/'.join([self._prefix, reponame, arch, rpm])
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('DELETE', path, headers=headers)
            response = httpServ.getresponse()
            httpServ.close()

            self.log.info("Propagated delete request %s and got response %d" % (path, response.status))

            if response.status != 204 and response.status != 404:
                raise Exception("Could not delete file %s on %s. Got response: %d" % (self._host, path, response.status))

    def propagate_rpm(self, source_repository, arch, file_name, destination_repository):
        if self._enabled:
            headers = {
                'User-Agent': self.USER_AGENT,
                'Content-Type': 'application/x-www-form-urlencoded',
                }
            source_path = '/'.join([source_repository, arch, file_name])
            postdata = 'source=%s&destination=%s' % (source_path, destination_repository)
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', '/propagation', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close();

            self.log.info("Propagated RPM propagation request from %s to %s and got response %d" % (source_path, destination_repository, response.status))

            if response.status != 201 and response.status != 404:
                raise Exception("Could not propagate")

    def propagate_repository(self, source_repository, destination_repository):
        if self._enabled:
            headers = {
                'User-Agent': self.USER_AGENT,
                'Content-Type': 'application/x-www-form-urlencoded',
                }
            postdata = 'source=%s&destination=%s' % (source_repository, destination_repository)
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', '/repo-propagation', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close();

            self.log.info("Propagated repository propagation request from %s to %s and got response %d" % (source_repository, destination_repository, response.status))

            if response.status != 201 and response.status != 404:
                raise Exception("Could not propagate")

    def uploadRpm(self, reponame, rpmPath):
        if self._enabled:
            c = pycurl.Curl()
            self.log.info("Try to upload rpm %s to repository %s" % (rpmPath, reponame))
            c.setopt(c.POST, 1)
            url = "http://%s/repo/%s/" % (self._host, reponame)
            self.log.info("Upload url: %s" % url)
            c.setopt(c.URL,url )
            c.setopt(c.HTTPPOST, [("rpmFile", (c.FORM_FILE, rpmPath))])
            c.setopt(pycurl.HTTPHEADER, ['User-Agent: ' + self.USER_AGENT])
            c.perform()
            returncode = c.getinfo(pycurl.HTTP_CODE)
            c.close()

            if returncode != httplib.CREATED:
                raise Exception("Upload failed.")