import string
import os

from piston.handler import BaseHandler
from yum_repo_server.static import serve
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from piston.utils import rc

class CsvListingHandler(BaseHandler):

    config = RepoConfigService()

    def read(self, request,repodir=''):
        
        okToRead=["virtual",""]
        if repodir in okToRead:
            response = rc.ALL_OK       
            response.content=""
            dir = None
            if repodir=='':
                dir=self.config.getStaticRepoDir()
            if repodir=='virtual':
                dir=self.config.getVirtualRepoDir()
            if not dir :
                response=rc.BAD_REQUEST
                response.write("")
                return respose       
            if os.path.exists(dir) and os.path.isdir(dir):     
                repos = os.listdir(dir)
                for repo in repos:
                    response.write(repo)
                    response.write("\n")
        else:
            response = rc.BAD_REQUEST
            response.write("""
            You are not allowed to look into %s"""%repodir)
        return response