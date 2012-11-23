import os
import re
import traceback

from piston.handler import BaseHandler
from piston.utils import rc

from yum_repo_server.api.services.repoPropagationService import RepoPropagationService
from yum_repo_server.api.services.repoAuditService import RepoAuditService


DESTINATION_KEY = 'destination'
SOURCE_KEY = 'source'


class ValidationException(BaseException):
    """
        to be raised when a validation error occurs
    """
    pass

class RpmPropagationHandler(BaseHandler):
    repoPropagationService = RepoPropagationService()
    audit = RepoAuditService()

    def _validate_post_data(self, data):
        if not data.has_key(SOURCE_KEY) or not data.has_key(DESTINATION_KEY):
            raise ValidationException('{0} and {1} attribute must be set.'.format(SOURCE_KEY, DESTINATION_KEY))

        if not re.match(r".+/.+/.+", data[SOURCE_KEY]):
            raise ValidationException('{0} format is not valid. (repo_name/architecture/rpm-name)'.format(SOURCE_KEY))

        destination_repo = data[DESTINATION_KEY]
        if '/' in destination_repo or str(destination_repo).startswith('.'):
            raise ValidationException('{0} must not contain slashes.'.format(DESTINATION_KEY))

    def create(self, request):
        data = request.POST

        try:
            self._validate_post_data(data)

            source_repo, package_architecture, package_name = data[SOURCE_KEY].split('/')
            destination_repo = data[DESTINATION_KEY]

            propagated_file_name = self.repoPropagationService.propagatePackage(package_name, source_repo, destination_repo, package_architecture)

            self.audit.log_action("propagated rpm %s/%s from %s to %s" % (package_architecture, package_name, source_repo, destination_repo), request)

            resp = rc.CREATED
            resp['Location'] = os.path.join('/repo', destination_repo, package_architecture, propagated_file_name)

            return resp

        except BaseException as e:
            return self._convert_exception_to_response(e)

    def _convert_exception_to_response(self, exception):
        """
            @return: BAD_REQUEST HTTP response with error message
        """
        resp = rc.BAD_REQUEST
        error_message = str(exception)
        stack_trace = traceback.format_exc()
        resp.content = "Message: {0}.\nTrace: {1}.".format(error_message, stack_trace)
        return resp
