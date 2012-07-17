from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.static import serve

import re
import os

class YumRepoHandler(BaseHandler):
    NAME_REGEX = '^[a-zA-Z0-9][a-zA-Z0-9_\-\.]*$'
    
    repoConfigService = RepoConfigService()

    def create(self, request, text):   
        data = request.POST

        if not data:
            resp = rc.BAD_REQUEST
            resp.content = 'POST data missing'
            return resp

        name = data.get('name', None) #set None as default if the key is missing
        if not name:
            resp = rc.BAD_REQUEST
            resp.content = 'The name attribute is missing'
            return resp

        if not re.match(self.NAME_REGEX, data['name']):
            resp = rc.BAD_REQUEST
            resp.content = 'The provided name is invalid. It must match this regular expression : ' + self.NAME_REGEX
            return resp
        
        if not os.path.exists(self.repoConfigService.getStaticRepoDir()):
            os.makedirs(self.repoConfigService.getStaticRepoDir())


        path = self.repoConfigService.getStaticRepoDir(name)
        if os.path.exists(path):
            resp = rc.DUPLICATE_ENTRY
            resp.content = 'The repository at ' + path + ' already exists.'
            return resp

        os.makedirs(path)

        resp = rc.CREATED
        resp.content = dict(
            name=request.POST['name'],
            dir=path
        )

        return resp

    def read(self, request, text):
	static_path = self.repoConfigService.getStaticRepoDir()
	if not os.path.exists(static_path):
	     os.makedirs(static_path)
        return serve(request, '/', static_path, True, True)
        
        
        
