import os

from unittest import TestCase
from mockito import when, verify, any as any_value, unstub
from yum_repo_server.api.services.repoPropagationService import RepoPropagationService, PropagationException
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoContentService import RepoContentService
from yum_repo_server.api.services.rpmService import RpmService
import yum_repo_server

class TestRepoPropagationService(TestCase):
    def setUp(self):
        self.service = RepoPropagationService()

    def tearDown(self):
        unstub()

    def test_should_propagate_package_using_rpm_service(self):
        package_name = "package_name"
        architecture = "architecture"

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(RepoPropagationService).determine_repository_path(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(RepoPropagationService).determine_repository_path(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)


        actual_file_name = self.service.propagatePackage(package_name, source_repository_name, destination_repository_name, architecture)


        self.assertEqual(package_name, actual_file_name)

        verify(RepoPropagationService).determine_repository_path(source_repository_name)
        verify(RepoPropagationService).determine_repository_path(destination_repository_name)

        verify(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(package_name)

        source_rpm_path = os.path.join(source_path, architecture, package_name)
        destination_rpm_parent_dir = os.path.join(destination_path, architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, package_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_rpm_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_rpm_parent_dir)

        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(source_rpm_path, destination_rpm_path)


    def test_should_propagate_package(self):
        architecture = "architecture"
        destination_path = "destination-static-repository-path"
        destination_repository_name = "destination-repository-name"
        full_package_name = "package-name-1-2-5.noarch.rpm"
        package_name = "package_name"
        source_path = "source-static-repository-path"
        source_repository_name = "source-repository-name"

        when(RepoPropagationService).determine_repository_path(source_repository_name).thenReturn(source_path)
        when(RepoPropagationService).determine_repository_path(destination_repository_name).thenReturn(destination_path)
        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(any_value()).thenReturn(None)
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)
        when(RpmService).get_latest_rpm(any_value(), any_value()).thenReturn(full_package_name)


        actual_file_name = self.service.propagatePackage(package_name, source_repository_name, destination_repository_name, architecture)


        self.assertEqual(full_package_name, actual_file_name)

        verify(RepoPropagationService).determine_repository_path(source_repository_name)
        verify(RepoPropagationService).determine_repository_path(destination_repository_name)

        verify(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(package_name)
        architecture_path = os.path.join(source_path, architecture)
        verify(RpmService).get_latest_rpm(package_name, architecture_path)

        source_rpm_path = os.path.join(source_path, architecture, full_package_name)
        destination_rpm_parent_dir = os.path.join(destination_path, architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, full_package_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_rpm_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_rpm_parent_dir)

        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(source_rpm_path, destination_rpm_path)

    def test_should_propagate_empty_repository(self):
        source_repository = "source-repo"
        destination_repository = "destination-repo"
        destination_repository_path = "destination-static-repository-path"

        when(RepoContentService).list_packages("source-repo").thenReturn([])
        when(RepoPropagationService).determine_repository_path(destination_repository).thenReturn(destination_repository_path)

        self.service.propagateRepository(source_repository, destination_repository)

    def test_should_propagate_repository_with_one_package(self):
        architecture = "arch1"
        destination_repository_path = "destination-static-repository-path"
        destination_repository = "destination-repo"
        package_path = os.path.join("source-repository-path", architecture, "spam.rpm")

        when(RepoPropagationService).determine_repository_path(destination_repository).thenReturn(destination_repository_path)
        when(RepoContentService).list_packages(any_value()).thenReturn([(architecture, package_path)])
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)


        self.service.propagateRepository("source-repo", destination_repository)


        verify(RepoPropagationService).determine_repository_path(destination_repository)
        verify(RepoContentService).list_packages("source-repo")
        destination_path = os.path.join(destination_repository_path, architecture)
        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(package_path, destination_path)

    def test_should_propagate_repository_with_two_packages(self):
        destination_repository_path = "destination-static-repository-path"
        destination_repository = "destination-repo"
        package_path1 = os.path.join("source-repository-path", "arch1", "spam.rpm")
        package_path2 = os.path.join("source-repository-path", "arch2", "egg.rpm")

        when(RepoPropagationService).determine_repository_path(destination_repository).thenReturn(destination_repository_path)
        when(RepoContentService).list_packages(any_value()).thenReturn([("arch1", package_path1), ("arch2", package_path2)])
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)


        self.service.propagateRepository("source-repo", destination_repository)


        verify(RepoPropagationService).determine_repository_path(destination_repository)
        verify(RepoContentService).list_packages("source-repo")
        destination_path1 = os.path.join(destination_repository_path, "arch1")
        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(package_path1, destination_path1)
        destination_path2 = os.path.join(destination_repository_path, "arch2")
        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(package_path2, destination_path2)

    def test_should_raise_exception_when_repository_path_does_not_exist(self):
        when(RepoConfigService).getStaticRepoDir("repository").thenReturn("path/to/repository")
        when(yum_repo_server.api.services.repoPropagationService.os.path).exists("path/to/repository").thenReturn(False)

        self.assertRaises(PropagationException, self.service.determine_repository_path, "repository")

    def test_should_return_repository_path(self):
        when(RepoConfigService).getStaticRepoDir("repository").thenReturn("path/to/repository")
        when(yum_repo_server.api.services.repoPropagationService.os.path).exists("path/to/repository").thenReturn(True)

        actual_path = self.service.determine_repository_path("repository")

        self.assertEqual("path/to/repository", actual_path)

        verify(RepoConfigService).getStaticRepoDir("repository")
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists("path/to/repository")
