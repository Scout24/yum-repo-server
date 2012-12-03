import os

from yum_repo_server.api.services.repoConfigService import RepoConfigService

class RepoContentService(object):
    """
        Service to retrieve information about the content of a repository.
    """
    METADATA_DIRECTORY = "repodata"

    repoConfigService = RepoConfigService()

    def list_architectures(self, repository_name):
        """
            @return: list of architecture paths
        """
        repository_path = self.repoConfigService.getStaticRepoDir(repository_name)
        directories_in_repository = os.listdir(repository_path)

        available_architectures = []

        for directory in directories_in_repository:
            architecture_path = os.path.join(repository_path, directory)

            if directory != self.METADATA_DIRECTORY:
                if os.path.isdir(architecture_path):
                    if not self._directory_is_empty(architecture_path):
                        available_architectures.append(architecture_path)

        return available_architectures

    def list_packages(self, repository_name):
        """
            @return: list of package paths
        """
        available_architectures = self.list_architectures(repository_name)
        packages_in_repository = []

        for architecture_path in available_architectures:
            packages_in_architecture_dir = os.listdir(architecture_path)

            for package in packages_in_architecture_dir:
                package_path = os.path.join(architecture_path, package)
                packages_in_repository.append(package_path)

        return packages_in_repository

    def _directory_is_empty(self, path):
        files_in_directory = os.listdir(path)
        return len(files_in_directory) == 0