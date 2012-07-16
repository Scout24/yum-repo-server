import os
import shutil
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from unittest import TestCase


class TestRepoConfigService(TestCase):
    targetDir = os.getcwd() + '/target/static/test-cleanup-dir'
    testRepo = targetDir + '/noarch'
    rpm_file_names = ['rss-4-1.2.noarch.rpm',
                      'rss-5-1.1.noarch.rpm',
                      'rss-7-1.4.noarch.rpm',
                      'rss-4-1.1.noarch.rpm']
    dict_rpm_tuples = {'rss':
                           [
                               ('4', '1.2', 'rss-4-1.2.noarch.rpm'),
                               ('5', '1.1', 'rss-5-1.1.noarch.rpm'),
                               ('7', '1.4', 'rss-7-1.4.noarch.rpm'),
                               ('4', '1.1', 'rss-4-1.1.noarch.rpm'),
                           ]}


    def test_doCleanup(self):
        os.makedirs(self.testRepo)
        self.touchRpms(self.testRepo)

        try:
            RepoConfigService().doCleanup(self.targetDir, 3)
            self.assertFalse(os.path.exists(self.testRepo + '/rss-4-1.1.noarch.rpm'))
        finally:
            shutil.rmtree(self.targetDir)
            
    def test_do_not_remove_rpms_when_rpm_max_keep_is_zero(self):
        os.makedirs(self.testRepo)
        self.touchRpms(self.testRepo)

        try:
            RepoConfigService().doCleanup(self.targetDir, 0)
            self.assertTrue(os.path.exists(self.testRepo + '/rss-4-1.1.noarch.rpm'), 'rpm-file was deleted.')
        finally:
            shutil.rmtree(self.targetDir)
        
    def test_do_cleanup_should_raise_valueError_when_rpm_max_keep_is_not_an_integer(self):
        try:
            RepoConfigService().doCleanup(self.targetDir, '3')
            self.fail('do cleanup should check, that rpm_max_keep is an integer.')
        except ValueError:
            pass

    def test_sort_of_obsolete_rpms(self):
        expected_list_to_delete = [('4', '1.1', 'rss-4-1.1.noarch.rpm')]
        list_to_delete = RepoConfigService()._check_dict_for_rpms_to_delete(self.dict_rpm_tuples, 3)

        self.assertEqual(expected_list_to_delete, list_to_delete)


    def test_get_rpm_group_with_obsolete_files_by_file_name(self):
        dict_group_with_obsolete_rpms = RepoConfigService()._get_rpm_group_with_obsolete_files_by_file_name(
            self.rpm_file_names, 3)

        self.assertEqual(self.dict_rpm_tuples.keys(), dict_group_with_obsolete_rpms.keys())
        self.assertEqual(self.dict_rpm_tuples.values(), dict_group_with_obsolete_rpms.values())

    def test_get_no_rpm_group_if_less_than_max_rpm(self):
        rpm_file_names = ['rss-4-1.1.noarch.rpm']
        dict_group_with_obsolete_rpms = RepoConfigService()._get_rpm_group_with_obsolete_files_by_file_name(
            rpm_file_names, 3)

        self.assertEqual([], dict_group_with_obsolete_rpms.keys())
        self.assertEqual([], dict_group_with_obsolete_rpms.values())


    def test_get_no_rpm_group_if_rpms_have_different_names(self):
        rpm_file_names = ['rss-4-1.1.noarch.rpm',
                          'rss-4-1.2.noarch.rpm',
                          'rss-5-1.1.noarch.rpm',
                          'feed-rss-7-1.4.noarch.rpm']
        dict_group_with_obsolete_rpms = RepoConfigService()._get_rpm_group_with_obsolete_files_by_file_name(
            rpm_file_names, 3)

        self.assertEqual([], dict_group_with_obsolete_rpms.keys())
        self.assertEqual([], dict_group_with_obsolete_rpms.values())

    def touchRpms(self, testRepo):
        for repo in self.rpm_file_names:
            open(testRepo + '/' + repo, 'w').close()
