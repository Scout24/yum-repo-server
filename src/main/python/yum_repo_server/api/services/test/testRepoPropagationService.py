import os

from unittest import TestCase
from mockito import when, verify, any as any_value, unstub
from yum_repo_server.api.services.repoPropagationService import RepoPropagationService, PropagationException
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.rpmService import RpmService
import yum_repo_server

class TestRepoPropagationService(TestCase):

    def tearDown(self):
        unstub()

    def test_should_raise_exception_when_source_repository_does_not_exist(self):
        package_name = "package_name"
        architecture = "architecture"

        service = RepoPropagationService()

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_path).thenReturn(False)


        self.assertRaises(PropagationException, service.propagatePackage, package_name, source_repository_name, destination_repository_name, architecture)


    def test_should_raise_exception_when_destination_repository_does_not_exist(self):
        package_name = "package_name"
        architecture = "architecture"

        service = RepoPropagationService()

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_path).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_path).thenReturn(False)


        self.assertRaises(PropagationException, service.propagatePackage, package_name, source_repository_name, destination_repository_name, architecture)


    def test_should_propagate_package_using_rpm_service(self):
        package_name = "package_name"
        architecture = "architecture"

        service = RepoPropagationService()

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)


        actual_file_name = service.propagatePackage(package_name, source_repository_name, destination_repository_name, architecture)


        self.assertEqual(package_name, actual_file_name)

        verify(RepoConfigService).getStaticRepoDir(source_repository_name)
        verify(RepoConfigService).getStaticRepoDir(destination_repository_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_path)

        verify(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(package_name)

        source_rpm_path = os.path.join(source_path, architecture, package_name)
        destination_rpm_parent_dir = os.path.join(destination_path, architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, package_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_rpm_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_rpm_parent_dir)

        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(source_rpm_path, destination_rpm_path)


    def test_should_propagate_package(self):
        package_name = "package_name"
        architecture = "architecture"

        service = RepoPropagationService()

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(RepoConfigService).getStaticRepoDir(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(any_value()).thenReturn(None)
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)
        full_package_name = "package-name-1-2-5.noarch.rpm"
        when(RpmService).get_latest_rpm(any_value(), any_value()).thenReturn(full_package_name)


        actual_file_name = service.propagatePackage(package_name, source_repository_name, destination_repository_name, architecture)


        self.assertEqual(full_package_name, actual_file_name)

        verify(RepoConfigService).getStaticRepoDir(source_repository_name)
        verify(RepoConfigService).getStaticRepoDir(destination_repository_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_path)

        verify(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(package_name)
        architecture_path = os.path.join(source_path, architecture)
        verify(RpmService).get_latest_rpm(package_name, architecture_path)

        source_rpm_path = os.path.join(source_path, architecture, full_package_name)
        destination_rpm_parent_dir = os.path.join(destination_path, architecture)
        destination_rpm_path = os.path.join(destination_rpm_parent_dir, full_package_name)

        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(source_rpm_path)
        verify(yum_repo_server.api.services.repoPropagationService.os.path).exists(destination_rpm_parent_dir)

        verify(yum_repo_server.api.services.repoPropagationService.shutil).move(source_rpm_path, destination_rpm_path)
