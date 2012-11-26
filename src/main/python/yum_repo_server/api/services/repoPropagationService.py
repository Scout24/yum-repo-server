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

    def propagatePackage(self, package_name, source_repository, destination_repository, architecture):

        source_repo_path = self.repoConfigService.getStaticRepoDir(source_repository)
        if not os.path.exists(source_repo_path):
            raise PropagationException("Static source repository '{0}' does not exist.".format(source_repository))

        destination_repo_path = self.repoConfigService.getStaticRepoDir(destination_repository)
        if not os.path.exists(destination_repo_path):
            raise PropagationException("Static destination repository '{0}' does not exist.".format(destination_repository))

        source_architecture_path = os.path.join(source_repo_path, architecture)
        file_name = self._determine_rpm_file_name(source_architecture_path, package_name)

        source_rpm_path = os.path.join(source_repo_path, architecture, file_name)
        destination_rpm_parent_dir = os.path.join(destination_repo_path, architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, file_name)

        if not os.path.exists(source_rpm_path):
            raise PropagationException("Package '{0}' could not be found.".format(source_rpm_path))

        if not os.path.exists(destination_rpm_parent_dir):
            os.mkdir(destination_rpm_parent_dir)

        shutil.move(source_rpm_path, destination_rpm_path)

        return file_name

    def _determine_rpm_file_name(self, directory, rpm):
        if create_rpm_file_object(rpm) is None:
            latest_rpm = self.rpmService.get_latest_rpm(rpm, directory)

            if latest_rpm is None:
                raise PropagationException("Package for {0} could not be found in {1}".format(rpm, directory))

            return latest_rpm

        return rpm


    def propagateRepository(self, source_repository, destination_repository):
        source_path = self.repoConfigService.getStaticRepoDir(source_repository)
        if not os.path.exists(source_path):
            raise PropagationException("Static source repository '{0}' does not exist.".format(source_repository))

        destination_path = self.repoConfigService.getStaticRepoDir(destination_repository)
        if not os.path.exists(destination_path):
            raise PropagationException("Static destination repository '{0}' does not exist.".format(destination_repository))

        architectures = os.listdir(source_path)
        if len(architectures) > 0:
            architecture = architectures[0]
            architecture_path = os.path.join(source_path, architecture)
            package = os.listdir(architecture_path)[0]

            source_package_path = os.path.join(architecture_path, package)
            destination_package_path = os.path.join(destination_path, architecture, package)
            shutil.move(source_package_path, destination_package_path)

