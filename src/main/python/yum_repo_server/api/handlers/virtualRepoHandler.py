from piston.handler import BaseHandler
from piston.utils import rc

import os
import shutil
import re
from django.http import HttpResponseRedirect
from yum_repo_server.static import serve
from yum_repo_server.api  import config
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoAuditService import RepoAuditService

class VirtualRepoHandler(BaseHandler):

    repoConfigService = RepoConfigService()
    audit = RepoAuditService()

    def read(self, request, reponame, rpm = '/'):
        virtual_reponame = reponame
        path_relative_to_repository = rpm

        virtual_repo_dir = self.repoConfigService.getVirtualRepoDir(virtual_reponame)
        if not os.path.exists(virtual_repo_dir):
            resp =  rc.NOT_FOUND
            resp.content = 'Virtual Repository does not exists!'
            return resp

        repoConfig = self.repoConfigService.getConfig(virtual_reponame)
        
        if re.match('^https?://.*', repoConfig.destination):
            return HttpResponseRedirect(repoConfig.destination + rpm) 
        else:
            absoluteDestinationRepoPath = config.get_repo_dir() + repoConfig.destination
            return serve(request, path_relative_to_repository, absoluteDestinationRepoPath, True)
    
    def delete(self, request, reponame, rpm = '/'):
        if not rpm is None and len(rpm) > 1:
            resp = rc.NOT_IMPLEMENTED
            resp.content = "You are just allowed to remove virtual repositories"
            return resp
        
        repo_path = self.repoConfigService.getVirtualRepoDir(reponame)
        if not os.path.exists(repo_path):
            resp =  rc.NOT_FOUND
            resp.content = 'Virtual Repository does not exists!'
            return resp
        
        self.audit.log_action("deleted virtual repository %s"%(reponame),request)
        shutil.rmtree(repo_path)
        return rc.DELETED
