from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api.services.mongo import MongoUpdater
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoAuditService import RepoAuditService
from yum_repo_server.static import serve, ParentDirType

import re
import os

class YumRepoHandler(BaseHandler):
    NAME_REGEX = '^[a-zA-Z0-9][a-zA-Z0-9_\-\.]*$'
    
    repoConfigService = RepoConfigService()
    audit = RepoAuditService()
    _mongo_updater = MongoUpdater() 

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
        
        path = self.repoConfigService.getStaticRepoDir(name)
        if os.path.exists(path):
            resp = rc.DUPLICATE_ENTRY
            resp.content = 'The repository at ' + path + ' already exists.'
            return resp

        os.makedirs(path)
        self._mongo_updater.createRepository(name)

        resp = rc.CREATED
        resp.content = dict(
            name=request.POST['name'],
            dir=path
        )

        self.audit.log_action("created static repository %s"%name,request)       
        return resp
    
    
    

    def read(self, request, text):
        static_path = self.repoConfigService.getStaticRepoDir()
        return serve(request=request, path='/', document_root=static_path, add_virtual=True, show_indexes=True, parent_dir_type=ParentDirType.STATIC)

        
        
        
