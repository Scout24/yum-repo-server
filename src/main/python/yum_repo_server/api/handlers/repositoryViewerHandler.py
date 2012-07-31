from piston.handler import BaseHandler
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.static import serve
from piston.utils import rc

class RepositoryViewerHandler(BaseHandler):
    
    def read(self, request, path):
        return serve(request, path, RepoConfigService().getStaticRepoDir(), True, False, False)
    
    def create(self, request, path):
        return rc.BAD_REQUEST
    
    def update(self, request, path):
        return rc.BAD_REQUEST

    def delete(self, request, path):
        return rc.BAD_REQUEST
