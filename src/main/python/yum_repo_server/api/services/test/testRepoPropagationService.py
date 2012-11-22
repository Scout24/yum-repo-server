from unittest import TestCase
from mockito import mock, when, verify, any as any_value, unstub
from yum_repo_server.api.services.repoPropagationService import RepoPropagationService
from yum_repo_server.api.services.repoConfigService import RepoConfigService

import yum_repo_server

class TestRepoPropagationService(TestCase):

    def tearDown(self):
        unstub()

    def test_should_propagate_package(self):
        service = RepoPropagationService()
        mockConfigService = mock(RepoConfigService)
        service.config = mockConfigService

        source_repository_name = "source-repository-name"
        source_path = "source-static-repository-path"
        when(mockConfigService).getStaticRepoDir(source_repository_name).thenReturn(source_path)

        destination_repository_name = "destination-repository-name"
        destination_path = "destination-static-repository-path"
        when(mockConfigService).getStaticRepoDir(destination_repository_name).thenReturn(destination_path)

        when(yum_repo_server.api.services.repoPropagationService.os.path).exists(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService).create_rpm_file_object(any_value()).thenReturn(True)
        when(yum_repo_server.api.services.repoPropagationService.shutil).move(any_value(), any_value()).thenReturn(None)


        actual_file_name = service.propagatePackage("package_name", source_repository_name, destination_repository_name, "architecture_name")


        self.assertEqual("package_name", actual_file_name)

