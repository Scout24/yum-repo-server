import os

from unittest import TestCase
from mockito import unstub, when, verify, never, any as any_value
from yum_repo_server.api.services.repoContentService import RepoContentService
from yum_repo_server.api.services.repoConfigService import RepoConfigService

import yum_repo_server

class TestRepoContentService(TestCase):
    def setUp(self):
        self.service = RepoContentService()

    def tearDown(self):
        unstub()

    def test_should_return_empty_dictionary_when_no_architectures_in_repository(self):
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn([])
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn("/path/to/repository")

        actual_architectures = self.service.list_architectures("testrepo")

        self.assertEqual([], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir("testrepo")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository")

    def test_should_return_empty_dictionary_when_no_packages_in_architecture_directories(self):
        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn("/path/to/repository")
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn(["architecture1"]).thenReturn([])

        actual_architectures = self.service.list_architectures("testrepo")

        self.assertEqual([], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir("testrepo")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture1")

    def test_should_return_empty_dictionary_when_repository_is_empty_but_directory_contains_file(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        architecture1 = "arch1"
        architecture2 = "arch2"
        architecture3 = "arch3"
        tags_file = "tags.txt"

        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn([architecture1, architecture2, architecture3, tags_file]).thenReturn([])
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture1)).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture2)).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture3)).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(tags_file).thenReturn(False)

        actual_architectures = self.service.list_architectures(repository)

        self.assertEqual([], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture1))
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture2))
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture3))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture1))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture2))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture3))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, tags_file))
        verify(yum_repo_server.api.services.repoContentService.os, never).listdir(os.path.join(repository_path, tags_file))

    def test_should_return_empty_dictionary_even_if_meta_data_has_been_generated(self):
        repository_path="/path/to/repository"
        repository = "testrepo"
        architecture1 = "arch1"
        architecture2 = "arch2"
        repodata_directory = "repodata"
        metadata_xml_file = "repomd.xml"

        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn(repository_path)
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn([architecture1, architecture2, repodata_directory, metadata_xml_file]).thenReturn([])
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture1)).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture2)).thenReturn(True)
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, repodata_directory)).thenReturn(True)

        actual_architectures = self.service.list_architectures(repository)

        self.assertEqual([], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir(repository)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(repository_path)
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture1))
        verify(yum_repo_server.api.services.repoContentService.os).listdir(os.path.join(repository_path, architecture2))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture1))
        verify(yum_repo_server.api.services.repoContentService.os.path).isdir(os.path.join(repository_path, architecture2))
        verify(yum_repo_server.api.services.repoContentService.os.path, never).isdir(os.path.join(repository_path, repodata_directory))

    def test_should_return_dictionary_with_one_architecture_when_second_directory_is_empty(self):

        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn("/path/to/repository")
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn(["architecture"]).thenReturn(["spam.rpm", "egg.rpm"])
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(any_value()).thenReturn(True)


        actual_architectures = self.service.list_architectures("testrepo")


        self.assertEqual(["/path/to/repository/architecture"], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir("testrepo")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture")


    def test_should_return_dictionary_with_two_architectures(self):

        when(RepoConfigService).getStaticRepoDir(any_value()).thenReturn("/path/to/repository")
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn(["architecture1", "architecture2"]).thenReturn(["spam1.rpm", "egg1.rpm"]).thenReturn(["spam2.rpm", "egg2.rpm"])
        when(yum_repo_server.api.services.repoContentService.os.path).isdir(any_value()).thenReturn(True)


        actual_architectures = self.service.list_architectures("testrepo")


        self.assertEqual(["/path/to/repository/architecture1", "/path/to/repository/architecture2"], actual_architectures)

        verify(RepoConfigService).getStaticRepoDir("testrepo")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture1")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture2")


    def test_should_return_empty_list_when_no_packages_in_repository(self):
        when(RepoContentService).list_architectures(any_value()).thenReturn([])

        actual_packages = self.service.list_packages("testrepo")

        self.assertEqual([], actual_packages)
        verify(RepoContentService).list_architectures(any_value())

    def test_should_return_one_package_from_architecture_directory(self):
        when(RepoContentService).list_architectures(any_value()).thenReturn(["/path/to/repository/architecture"])
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn(["spam.rpm"])

        actual_packages = self.service.list_packages("testrepo")


        self.assertEqual(["/path/to/repository/architecture/spam.rpm"], actual_packages)

        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture")

    def test_should_return_two_packages_from_one_architecture_directory(self):
        when(RepoContentService).list_architectures(any_value()).thenReturn(["/path/to/repository/architecture"])
        when(yum_repo_server.api.services.repoContentService.os).listdir(any_value()).thenReturn(["spam.rpm", "egg.rpm"])

        actual_packages = self.service.list_packages("testrepo")


        self.assertEqual(["/path/to/repository/architecture/spam.rpm", "/path/to/repository/architecture/egg.rpm"], actual_packages)

        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture")

    def test_should_return_two_packages_from_multiple_architecture_directories(self):
        when(RepoContentService).list_architectures(any_value()).thenReturn(["/path/to/repository/architecture1", "/path/to/repository/architecture2"])
        when(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture1").thenReturn(["spam.rpm"])
        when(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture2").thenReturn(["egg.rpm"])

        actual_packages = self.service.list_packages("testrepo")

        self.assertEqual(["/path/to/repository/architecture1/spam.rpm", "/path/to/repository/architecture2/egg.rpm"], actual_packages)

        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture1")
        verify(yum_repo_server.api.services.repoContentService.os).listdir("/path/to/repository/architecture2")

