import os
import shutil

from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoContentService import RepoContentService
from yum_repo_server.api.services.rpmService import create_rpm_file_object, RpmService

class PropagationException(BaseException):
    """
        to be raised when a propagation error occurs
    """
    pass

class RepoPropagationService(object):
    repoConfigService = RepoConfigService()
    repoContentService = RepoContentService()
    rpmService = RpmService()

    def propagatePackage(self, package_name, source_repository, destination_repository, architecture):
        source_repo_path = self.determine_repository_path(source_repository)
        destination_repo_path = self.determine_repository_path(destination_repository)

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
        destination_repository_path = self.determine_repository_path(destination_repository)

        packages_to_propagate = self.repoContentService.list_packages(source_repository)

        for package_architecture, package_path in packages_to_propagate:
            destination_path = os.path.join(destination_repository_path, package_architecture)

            shutil.move(package_path, destination_path)

    def determine_repository_path(self, repository_name):
        repository_path = self.repoConfigService.getStaticRepoDir(repository_name)

        if not os.path.exists(repository_path):
            raise PropagationException("Static repository '{0}' does not exist.".format(repository_name))

        return repository_path

