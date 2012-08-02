import string
from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoTaggingService import RepoTaggingService,CouldNotLockTagsException,NotFoundException,NoSuchTagException
import re


class RequestFailException(Exception):
    pass


class RepoTaggingHandler(BaseHandler):

    repoConfigService = RepoConfigService()
    repoTaggingService = RepoTaggingService()

    def create(self, request, repodir,tag):
        try:
            tag = self.check_request_sanity(request)
        except RequestFailException, rfe:
            return rfe.args[0]

        try:
            result = self.repoTaggingService.tagRepo(repodir,tag)
        except CouldNotLockTagsException, lockErr:
            response = rc.BAD_REQUEST
            response.content = "Could not lock tags file"
            return response
        response = rc.CREATED
        response.content = result
        return response


    def check_request_sanity(self, request):
        data = request.POST
        if not data:
            resp = rc.BAD_REQUEST
            resp.content = 'POST data missing'
            raise (RequestFailException(resp))

        tag = data.get('tag', None)

        if not tag:
            resp = rc.BAD_REQUEST
            resp.content = 'The tag attribute is missing'
            raise (RequestFailException(resp))
        if not re.match("^[\w\-\.]+$", tag):
            resp = rc.BAD_REQUEST
            resp.content = 'The tag attribute did not match the required pattern'
            raise (RequestFailException(resp))
        return tag

    # handle GET requests
    def read(self, request, repodir,tag):
      try:
        tags = self.repoTaggingService.getTags(repodir)
      except NotFoundException, e:
        response=rc.NOT_FOUND
        response.content="The repository '"+repodir+"' does not exist"
        return response
      response = rc.ALL_OK
      response.content = string.join(tags, '\n')
      return response
            


    def delete(self,request, repodir,tag):
        try:
            result = self.repoTaggingService.unTagRepo(repodir,tag)
        except CouldNotLockTagsException, lockErr:
            response = rc.BAD_REQUEST
            response.content = "Could not lock tags file"
            return response
        except NoSuchTagException, noTagErr:
            response = rc.NOT_FOUND
            response.content = "The repository "+repodir+" has no tag "+tag
            return response
        response = rc.DELETED
        response.content = result
        return response
