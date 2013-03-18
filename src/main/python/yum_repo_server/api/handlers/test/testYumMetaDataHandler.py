import os

from unittest import TestCase
from mockito import mock, when, verify, unstub, any as any_value
from yum_repo_server.api.handlers.yumMetaDataHandler import YumMetaDataHandler

class TestYumMetaDataHandler(TestCase):

    def setUp(self):
        self._handler = YumMetaDataHandler()
        self._handler.mongoUpdater = mock()
        self._handler.repoConfigService = mock()

    def tearDown(self):
        unstub()

    def test_should_propagate_generate_metadata(self):
        repo_name = 'repo_name'

        when(self._handler.repoConfigService).getStaticRepoDir(repo_name).thenReturn('any')
        when(os.path).exist('any').thenReturn(False)
        when(self._handler.mongoUpdater).generate_meta_data('dd').thenReturn(None)

        self._handler.create(None, repo_name)

        verify(self._handler.mongoUpdater).generate_meta_data('dd')
