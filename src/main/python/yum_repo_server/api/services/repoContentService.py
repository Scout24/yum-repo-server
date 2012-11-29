import os

from yum_repo_server.api.services.repoConfigService import RepoConfigService

class RepoContentService(object):
    """
        Service to retrieve information about the content of a repository.
    """

    repoConfigService = RepoConfigService()

    def list_packages(self, repository_name):
        """
            @return: a list of tuples (first element is the architecture name, second element is the package file name)
        """
        repository_path = self.repoConfigService.getStaticRepoDir(repository_name)

        files_in_repository = os.listdir(repository_path)

        if self.repoConfigService.TAG_FILENAME in files_in_repository:
            files_in_repository.remove(self.repoConfigService.TAG_FILENAME )

        if self.repoConfigService.METADATA_GENERATION_FILENAME in files_in_repository:
            files_in_repository.remove(self.repoConfigService.METADATA_GENERATION_FILENAME)

        available_architectures = files_in_repository
        packages_in_repository = []

        for architecture in available_architectures:
            architecture_path = os.path.join(repository_path, architecture)
            packages_in_architecture_dir = os.listdir(architecture_path)

            for package in packages_in_architecture_dir:
                package_path = os.path.join(repository_path, architecture, package)
                packages_in_repository.append((architecture, package_path))

        return packages_in_repository