import os
from yum_repo_server.static import serve
from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api import config
from yum_repo_server.api.services.repoConfigService import RepoConfigService


class YumMetaDataHandler(BaseHandler):
    repoConfigService = RepoConfigService()
    
    def create(self, request, reponame):

        full_path_to_repo = self.repoConfigService.getStaticRepoDir(reponame)

        if not os.path.exists(full_path_to_repo):
            resp = rc.NOT_FOUND
            resp.content = 'Repository %s not found' % full_path_to_repo
            return resp


        try:
            self.repoConfigService.doCreateRepo(full_path_to_repo, reponame)
        except:
            resp = rc.BAD_REQUEST
            resp.content = 'createrepo has finished with Error'
            return resp

        resp = rc.CREATED
        resp.content = config.get_repo_dir()
        return resp
    
    def read(self, request, reponame):
        return serve(request, '/' + reponame + '/repodata/', self.repoConfigService.getStaticRepoDir(), True)
        