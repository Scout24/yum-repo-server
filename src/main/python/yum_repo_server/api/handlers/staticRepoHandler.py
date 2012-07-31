import os

from piston.handler import BaseHandler
from yum_repo_server.static import serve
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from piston.utils import rc

class StaticRepoHandler(BaseHandler):

    config = RepoConfigService()

    def read(self, request, reponame, arch, rpm):
        rpm_path = os.path.join(reponame, arch, rpm)
        return serve(request, rpm_path, self.config.getStaticRepoDir(), True, False, False)

    def delete(self, request, reponame, arch, rpm):
        path_variables = reponame + arch + rpm
        if '/' in path_variables or '..' in path_variables:
            return self._bad_request('')
        
        repository_path = self.config.getStaticRepoDir(reponame)
        if not os.path.isdir(repository_path):
            return rc.NOT_FOUND
        
        rpm_path = os.path.join(repository_path, arch, rpm)
        
        if not rpm_path.endswith('.rpm'):
            return self._bad_request('rpm name has to end with .rpm')

        if not os.path.isfile(rpm_path):
            return rc.NOT_FOUND
        
        os.remove(rpm_path)
        return rc.DELETED

    def create(self, request, path):
        return rc.BAD_REQUEST

    def update(self, request, path):
        return rc.BAD_REQUEST

    def _bad_request(self, message):
        response = rc.BAD_REQUEST
        response.write(message)
        return response
