import httplib
import logging
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

    def propagateRpm(self, source_repository, arch, file_name, destination_repository):
        if self._enabled:
            headers = {
                'User-Agent': self.USER_AGENT,
                'Content-Type': 'application/x-www-form-urlencoded',
                }
            sourcePath = '/'.join([source_repository, arch, file_name])
            postdata = 'source=' + sourcePath + '&destination=' + destination_repository
            httpServ = httplib.HTTPConnection(self._host)
            httpServ.connect()
            httpServ.request('POST', '/propagation', postdata, headers)
            response = httpServ.getresponse()
            httpServ.close();

            self.log.info("Propagated RPM propagation request %s to %s and got response %d" % (sourcePath, destination_repository, response.status))

            if response.status != 201 and response.status != 404:
                raise Exception("Could not propagate")






