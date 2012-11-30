import traceback

from piston.utils import rc
from piston.handler import BaseHandler

from yum_repo_server.api.services.repoAuditService import RepoAuditService
from yum_repo_server.api.services.repoPropagationService import RepoPropagationService

SOURCE_KEY = "source"
DESTINATION_KEY = "destination"


class ValidationException(BaseException):
    """
        to be raised when a validation error occurs
    """
    pass

class RepoPropagationHandler(BaseHandler):
    repoPropagationService = RepoPropagationService()
    repoAuditService = RepoAuditService()

    def create(self, request):
        try:
            data = request.POST

            self._validate_post_data(data)

            source_repository = data[SOURCE_KEY]
            destination_repository = data[DESTINATION_KEY]

            propagated_packages = self.repoPropagationService.propagate_repository(source_repository, destination_repository)
            message = "Propagated repository {0} to {1}, packages: {2}".format(source_repository, destination_repository, propagated_packages)
            self.repoAuditService.log_action(message, request)

            return rc.CREATED

        except BaseException as exception:
            return self.convert_exception_to_response(exception)

    def _validate_post_data(self, data):
        self._validate_repository_parameter(SOURCE_KEY, data)
        self._validate_repository_parameter(DESTINATION_KEY, data)

    def _validate_repository_parameter(self, parameter_name, data):
        if not data.has_key(parameter_name):
            raise ValidationException('"{0}" parameter must be set.'.format(parameter_name))

        repository_name = data[parameter_name]
        if '/' in repository_name or str(repository_name).startswith('.'):
            raise ValidationException(
                '{0} repository name {1} is not valid: must not contain "/" or start with "."'.format(parameter_name, repository_name))

    def convert_exception_to_response(self, exception):
        """
            @return: BAD_REQUEST HTTP response with error message
        """
        resp = rc.BAD_REQUEST
        error_message = str(exception)
        stack_trace = traceback.format_exc()
        resp.content = "Message: {0}.\nTrace: {1}.".format(error_message, stack_trace)
        return resp


