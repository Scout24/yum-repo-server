import os
import shutil

from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.rpmService import create_rpm_file_object, RpmService

class PropagationException(BaseException):
    """
        to be raised when a propagation error occurs
    """
    pass

class RepoPropagationService(object):
    repoConfigService = RepoConfigService()
    rpmService = RpmService()

    def propagatePackage(self, package_name, source_repository_name, destination_repository_name, package_architecture):
        source_repo_path = self.repoConfigService.getStaticRepoDir(source_repository_name)
        destination_repo_path = self.repoConfigService.getStaticRepoDir(destination_repository_name)

        if not os.path.exists(source_repo_path):
            raise PropagationException('source repository does not exist.')

        if not os.path.exists(destination_repo_path):
            raise PropagationException('destination repository does not exist')

        source_architecture_path = os.path.join(source_repo_path, package_architecture)
        file_name = self._determine_rpm_file_name(source_architecture_path, package_name)

        source_rpm_path = os.path.join(source_repo_path, package_architecture, file_name)
        destination_rpm_parent_dir = os.path.join(destination_repo_path, package_architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, file_name)

        if not os.path.exists(source_rpm_path):
            raise PropagationException('rpm_file could not be found.')

        if not os.path.exists(destination_rpm_parent_dir):
            os.mkdir(destination_rpm_parent_dir)

        shutil.move(source_rpm_path, destination_rpm_path)

        return file_name

    def _determine_rpm_file_name(self, directory, rpm):
        if create_rpm_file_object(rpm) is None:
            latest_rpm = self.rpmService.get_latest_rpm(rpm, directory)

            if latest_rpm is None:
                raise PropagationException('rpm file for {0} could not be found in {1}'.format(rpm, directory))

            return latest_rpm

        return rpm


