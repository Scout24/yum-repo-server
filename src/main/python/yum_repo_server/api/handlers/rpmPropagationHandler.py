import os
import re
import shutil

from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoAuditService import RepoAuditService

from yum_repo_server.api.services.rpmService import create_rpm_file_object, RpmService

DESTINATION_KEY = 'destination'
SOURCE_KEY = 'source'

class PropagationException(BaseException):
    """
        to be raised when a propagation error occurs
    """
    pass

class RpmPropagationHandler(BaseHandler):

    config = RepoConfigService()
    audit = RepoAuditService()

    def _validate_post_data(self, data):
        if not data.has_key(SOURCE_KEY) or not data.has_key(DESTINATION_KEY):
            raise PropagationException('{0} and {1} attribute must be set.'.format(SOURCE_KEY, DESTINATION_KEY))

        if not re.match(r".+/.+/.+", data[SOURCE_KEY]):
            raise PropagationException('{0} format is not valid. (repo_name/architecture/rpm-name)'.format(SOURCE_KEY))

        destination = data[DESTINATION_KEY]
        if '/' in destination or str(destination).startswith('.'):
            raise PropagationException('{0} must not contain slashes.'.format(DESTINATION_KEY))


    def create(self, request):
        data = request.POST

        try:
            self._validate_post_data(data)

            source_repo, source_arch, rpm = data[SOURCE_KEY].split('/')
            source_repo_path = self.config.getStaticRepoDir(source_repo)

            if not os.path.exists(source_repo_path):
                raise PropagationException('{0} repository does not exist.'.format(SOURCE_KEY))

            destination = data[DESTINATION_KEY]

            destination_repo_path = self.config.getStaticRepoDir(destination)
            if not os.path.exists(destination_repo_path):
                raise PropagationException('{0} repository does not exist'.format(DESTINATION_KEY))

            rpm = self._determine_rpm_file_name(os.path.join(source_repo_path, source_arch), rpm)

            source_rpm_path = os.path.join(source_repo_path, source_arch, rpm)
            destination_rpm_path = os.path.join(destination_repo_path, source_arch, rpm)

            if not os.path.exists(source_rpm_path):
                raise PropagationException('rpm_file could not be found.')

            destination_rpm_parent_dir = os.path.dirname(destination_rpm_path)

            if not os.path.exists(destination_rpm_parent_dir):
                os.mkdir(destination_rpm_parent_dir)

            self.audit.log_action("propagated rpm %s/%s from %s to %s" % (source_arch, rpm, source_repo, destination), request)
            shutil.move(source_rpm_path, destination_rpm_path)

            resp = rc.CREATED
            resp['Location'] = os.path.join('/repo', data[DESTINATION_KEY], source_arch, rpm)

            return resp

        except BaseException as e:
            return self._convert_exception_to_response(e)


    def _determine_rpm_file_name(self, directory, rpm):
        result = None

        if create_rpm_file_object(rpm) is None:
            result = RpmService().get_latest_rpm(rpm, directory)
        else:
            result = rpm

        if result is None:
            raise PropagationException('rpm_file could not be found.')

        return result


    def _convert_exception_to_response(self, exception):
        """
            @return: BAD_REQUEST HTTP response with error message
        """
        resp = rc.BAD_REQUEST
        resp.content = str(exception)
        return resp
