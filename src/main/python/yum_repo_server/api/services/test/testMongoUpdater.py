import httplib

from unittest import TestCase
from httplib import HTTPConnection
from yum_repo_server.api.services.mongo import MongoUpdater
from mockito import mock, when, verify, unstub, any as any_value

class TestMongoUpdater(TestCase):
    _default_headers = {'User-Agent': 'Yum-Repo-Server 1.0'}
    _default_host = 'default_host'
    _default_prefix = '/default_prefix'

    def setUp(self):
        self._mongo_updater = MongoUpdater()
        self._mongo_updater._host = self._default_host
        self._mongo_updater._prefix = self._default_prefix
        self._mongo_updater._enabled = True

    def tearDown(self):
        unstub()

    def test__default_headers_are_present(self):
        actual_headers = self._mongo_updater._create_headers()

        self.assertEqual(self._default_headers, actual_headers)

    def test_custom_headers_are_present(self):
        custom_header = {'key': 'value'}
        expected_header = dict(self._default_headers)
        expected_header.update(custom_header)

        actual_headers = self._mongo_updater._create_headers(custom_header)

        self.assertEqual(expected_header, actual_headers)

    def test_request_response_host_is_used(self):
        when(httplib).HTTPConnection(self._default_host).thenReturn(mock(HTTPConnection))

        self._mongo_updater._request_response('GET', None)

        verify(httplib).HTTPConnection(self._default_host)

    def test_request_response_correct_parameters_are_used(self):
        method = 'any_method'
        url = 'any_url'
        header = 'any_header'
        http_connection_mock = mock(HTTPConnection)

        when(httplib).HTTPConnection(any_value()).thenReturn(http_connection_mock)
        when(http_connection_mock).request(method, url, None, header).thenReturn(None)

        self._mongo_updater._request_response(method, url, header)

        verify(http_connection_mock).request(method, url, None, header)

    def test_request_response_returns_response(self):
        response = 'any_response'
        http_connection_mock = mock(HTTPConnection)

        when(httplib).HTTPConnection(any_value()).thenReturn(http_connection_mock)
        when(http_connection_mock).getresponse().thenReturn(response)

        actual_response = self._mongo_updater._request_response('any_method', 'any_url')

        self.assertEqual(response, actual_response)

    def test_create_repo_url(self):
        actual_repo_url = self._mongo_updater._create_repo_url('repo_name', 'action')

        self.assertEqual(self._default_prefix + '/repo_name/action', actual_repo_url)

    def test_create_repo_url_without_action(self):
        actual_repo_url = self._mongo_updater._create_repo_url('repo_name')

        self.assertEqual(self._default_prefix + '/repo_name/', actual_repo_url)

    def test_generate_meta_data(self):
        repo_name = 'any_repo_name'
        response_mock = mock()
        response_mock.status = 201
        when(self._mongo_updater)._request_response(
            'POST', self._create_generate_metata_data_url(repo_name), any_value()
        ).thenReturn(response_mock)

        self._mongo_updater.generate_meta_data(repo_name)

        verify(self._mongo_updater)._request_response(
            'POST', self._create_generate_metata_data_url(repo_name), any_value()
        )

    def test_generate_meta_data_raises_exception_for_response_codes_othen_than_201(self):
        repo_name = 'any_repo_name'
        response_mock = mock()
        response_mock.status = 200
        when(self._mongo_updater)._request_response(
            'POST', self._create_generate_metata_data_url(repo_name), any_value()
        ).thenReturn(response_mock)

        self.assertRaises(Exception, self._mongo_updater.generate_meta_data, repo_name)

    def _create_generate_metata_data_url(self, repo_name):
        return self._default_prefix + '/' + repo_name + '/repodata'

