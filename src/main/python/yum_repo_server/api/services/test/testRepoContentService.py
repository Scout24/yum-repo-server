import os

from unittest import TestCase
from mockito import unstub, when, verify, any as any_value
from yum_repo_server.api.services.repoContentService import RepoContentService
from yum_repo_server.api.services.repoConfigService import RepoConfigService

import yum_repo_server

class TestRepoContentService(TestCase):
    def setUp(self):
        self.service = RepoContentService()

    def tearDown(self):
        unstub()

    def test_should_return_empty_list_when_no_packages_in_repository(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        architecture1 = "arch1"
        architecture2 = "arch2"
        architecture3 = "arch3"

        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn([architecture1, architecture2, architecture3]).thenReturn([])
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)

        actual_packages = self.service.list_packages(repository)

        self.assertEqual([], actual_packages)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture1))
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture2))
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture3))

    def test_should_return_one_package_from_archicture_directory(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        package = "spam.rpm"
        architecture = "arch1"
        architecture_path = os.path.join(repository_path, architecture)

        when(yum_repo_server.api.services.repoContentService.os).listdir(repository_path).thenReturn([architecture])
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)
        when(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path).thenReturn([package])


        actual_packages = self.service.list_packages(repository)


        self.assertEqual([(architecture, os.path.join(repository_path, architecture, package))], actual_packages)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path)

    def test_should_return_two_packages_from_architecture_directory(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        package1 = "spam.rpm"
        package2 = "egg.rpm"
        packages = [package1, package2]
        architecture = "arch1"
        architecture_path = os.path.join(repository_path, architecture)
        path_to_package2 = os.path.join(repository_path, architecture, package2)
        path_to_package1 = os.path.join(repository_path, architecture, package1)

        when(yum_repo_server.api.services.repoContentService.os).listdir(repository_path).thenReturn([architecture])
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)
        when(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path).thenReturn(packages)


        actual_packages = self.service.list_packages(repository)

        self.assertEqual([(architecture, path_to_package1), (architecture, path_to_package2)], actual_packages)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path)

    def test_should_return_two_packages_from_two_different_architecture_directories(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        package1 = "spam.rpm"
        package2 = "egg.rpm"
        architecture1 = "arch1"
        architecture_path1 = os.path.join(repository_path, architecture1)
        architecture2 = "arch2"
        architecture_path2 = os.path.join(repository_path, architecture2)
        path_to_package1 = os.path.join(repository_path, architecture1, package1)
        path_to_package2 = os.path.join(repository_path, architecture2, package2)

        when(yum_repo_server.api.services.repoContentService.os).listdir(repository_path).thenReturn([architecture1, architecture2])
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)
        when(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path1).thenReturn([package1])
        when(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path2).thenReturn([package2])


        actual_packages = self.service.list_packages(repository)


        self.assertEqual([(architecture1, path_to_package1), (architecture2, path_to_package2)], actual_packages)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path1)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(architecture_path2)
