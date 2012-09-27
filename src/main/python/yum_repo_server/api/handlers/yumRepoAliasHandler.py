from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api import config
from yum_repo_server.api.services.repoConfigService import RepoConfigService, \
    RepoNotFoundException
from yum_repo_server.api.services.repoAuditService import RepoAuditService
from yum_repo_server.static import serve, ParentDirType


class RequestFailException(Exception):
    pass


class YumRepoAliasHandler(BaseHandler):

    POST_PARAM_DESTINATION_NAME = 'destination'
    POST_PARAM_VIRTUAL_REPO_NAME = 'name'

    repoConfigService = RepoConfigService()
    repoAuditService = RepoAuditService()

    def create(self, request, text):
        try:
            virtual_repo_name, destination_repo = self.check_request_sanity(request)
        except RequestFailException, rfe:
            return rfe.args[0]

        try: 
            result = self.repoConfigService.createVirtualRepo(virtual_repo_name, destination_repo)
        except RepoNotFoundException:
            resp = rc.NOT_HERE #Do NOT disclose the actual path to the client -> return relative destination
            resp.content = 'The destination repository at %s does not exist.' % destination_repo
            return resp

        self.repoAuditService.log_action("created a virtual link from %s to %s"%(virtual_repo_name, destination_repo),request)
        response = rc.CREATED
        response.content = result
        return response


    def check_request_sanity(self, request):
        data = request.POST
        if not data:
            resp = rc.BAD_REQUEST
            resp.content = 'POST data missing'
            raise (RequestFailException(resp))

        name = data.get(self.POST_PARAM_VIRTUAL_REPO_NAME, None)

        if not name:
            resp = rc.BAD_REQUEST
            resp.content = 'The name attribute is missing'
            raise (RequestFailException(resp))

        destination_repo = data.get(self.POST_PARAM_DESTINATION_NAME, None)

        if not destination_repo:
            resp = rc.BAD_REQUEST
            resp.content = 'The destination attribute is missing'
            raise (RequestFailException(resp))
        
        return name, destination_repo

    # handle GET requests
    def read(self, request, text):
        return serve(request=request, path='/virtual/', document_root=config.get_repo_dir(), show_indexes=True, parent_dir_type=ParentDirType.VIRTUAL)
            



        
