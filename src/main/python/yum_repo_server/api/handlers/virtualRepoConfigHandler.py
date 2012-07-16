from piston.handler import BaseHandler
from piston.utils import rc

import os
import shutil
import json
from yum_repo_server.static import serve
from yum_repo_server.api  import config
from yum_repo_server.api.services.repoConfigService import RepoConfigService

class VirtualRepoConfigHandler(BaseHandler):
    
    repoConfigService = RepoConfigService()
    
    def read(self, request, reponame):
        if '../' in reponame:
            return rc.BAD_REQUESST
        repo_dir = self.repoConfigService.getVirtualRepoDir(reponame)
        if not os.path.exists(repo_dir):
            return rc.NOT_FOUND
        
        repo_config = self.repoConfigService.getConfig(reponame)
        
        resp = rc.ALL_OK
        resp.content = ''
        json.dump(repo_config.data, resp)
        return resp
        
        