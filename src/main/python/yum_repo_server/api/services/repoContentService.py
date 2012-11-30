import os

from yum_repo_server.api.services.repoConfigService import RepoConfigService

class RepoContentService(object):
    """
        Service to retrieve information about the content of a repository.
    """

    repoConfigService = RepoConfigService()

    def list_architectures(self, repository_name):
        """
            @return: a dictionary which maps the architecture name to the corresponding repository path
        """
        repository_path = self.repoConfigService.getStaticRepoDir(repository_name)
        files_in_repository = os.listdir(repository_path)

        available_architectures = {}

        for potential_dir in files_in_repository:
            architecture_path = os.path.join(repository_path, potential_dir)
            if potential_dir != "repodata" and os.path.isdir(architecture_path):
                available_architectures[potential_dir] = architecture_path

        return available_architectures

    def list_packages(self, repository_name):
        """
            @return: a list of tuples (first element is the architecture name, second element is the package file name)
        """
        available_architectures = self.list_architectures(repository_name)
        packages_in_repository = []

        for architecture in available_architectures:
            architecture_path = available_architectures[architecture]
            packages_in_architecture_dir = os.listdir(architecture_path)

            for package in packages_in_architecture_dir:
                package_path = os.path.join(architecture_path, package)
                packages_in_repository.append((architecture, package_path))

        return packages_in_repository