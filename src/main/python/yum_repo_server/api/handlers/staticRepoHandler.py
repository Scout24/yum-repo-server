import string
import os

from piston.handler import BaseHandler
from yum_repo_server.static import serve
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from piston.utils import rc

class StaticRepoHandler(BaseHandler):

    config = RepoConfigService()

    def read(self, request, path):
        return serve(request, path, self.config.getStaticRepoDir(), True, False, False)

    def delete(self, request, path):
        if '../' in path:
            return self.badRequest('../ in path not allowed')
        parts = path.split('/')
        reponame = parts[0]
        rpm = string.join(parts[1:], '/')
        if not rpm.endswith('.rpm'):
            return self.badRequest('rpm name has to end with .rpm')

        repositoryPath = self.config.getStaticRepoDir(reponame)
        if not os.path.isdir(repositoryPath):
            return rc.NOT_FOUND
        fullPathToRpm = os.path.join(repositoryPath, rpm)
        if not os.path.isfile(fullPathToRpm):
            return rc.NOT_FOUND
        os.remove(fullPathToRpm)
        return rc.DELETED

    def create(self, request, path):
        return rc.BAD_REQUEST

    def update(self, request, path):
        return rc.BAD_REQUEST

    def badRequest(self, message):
        response = rc.BAD_REQUEST
        response.write(message)
        return response