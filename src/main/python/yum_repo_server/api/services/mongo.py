import httplib
from yum_repo_server.api import config

class MongoUpdater():

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

            if response.status != 204 and response.status != 404:
                raise Exception("Could not delete file %s on %s. Got response: %d" % (self._host, path, response.status))




