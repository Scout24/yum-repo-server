
import unittest
import os
import subprocess
import time
import shutil
import stat

class TestStartWorkingSchedulerDaemon(unittest.TestCase):
    PID_FILE_PATH = os.path.abspath('target/testpidfile.pid')
    
    def setUp(self):
        if not os.path.exists('target/static'):
            os.makedirs("target/static")
    
    def test_pidfile_parameter_is_used_and_pidfile_is_deleted_on_shutdown(self):
        def action():
            self.assertTrue(wait_for(self._pid_file_created, 10), 'pidfile was not created within 10 seconds.')
        
        self._safely_execute_with_daemon(action)
            
        self.assertTrue(self._pid_file_deleted, 'pidfile was not deleted in 10 seconds.')
    
    def test_createrepo_is_executed(self):
        createrepo_dir = 'target/static/createrepo_test'
        def action():
            self.assertTrue(wait_for(lambda: os.path.exists(createrepo_dir + '/repodata'), 10), 'repodata was not created within 10 seconds.')
        
        self._given_repo_dir_from_test_resources('daemon/createrepo', createrepo_dir)
        
        self._safely_execute_with_daemon(action)

    def test_other_can_not_write_repodata(self):
        createrepo_dir = 'target/static/createrepo_test'
        def action():
            self.assertTrue(wait_for(lambda: os.path.exists(createrepo_dir + '/repodata'), 10), 'repodata was not created within 10 seconds.')
            self.assertFalse(self.is_other_writeable(createrepo_dir + '/repodata'), 'daemon umask is not correct, OTHER has write permissions on repodata!')
        
        self._given_repo_dir_from_test_resources('daemon/createrepo', createrepo_dir)

        self._safely_execute_with_daemon(action)
        
    def is_other_writeable(self,path):
        st = os.stat(path)
        can_write = bool(st.st_mode & stat.S_IWOTH) 
        print "can write : %s"%can_write
        return can_write 

    def test_cleanup_rpm_job_is_executed(self):
        repo_dir = 'target/static/cleanup_test'
        rpm_to_delete = repo_dir + '/noarch/test-rpm-3.2-12.noarch.rpm'
        
        def action():
            self.assertTrue(wait_for(lambda: not os.path.exists(rpm_to_delete), 10), 'rpm was not deleted in time.')
        
        self._given_repo_dir_from_test_resources('daemon/cleanup_test', repo_dir)
        self.assertTrue(os.path.exists(rpm_to_delete), 'rpm to delete does not exist.')
        
        self._safely_execute_with_daemon(action)

    def _safely_execute_with_daemon(self, action):
        self._ensure_pid_file_is_deleted()
        self.assertEqual(0, subprocess.call(['python', 'localDaemon.py', 'start', '--pidfile=' + self.PID_FILE_PATH]))
        
        try:
            action()
        finally:
            self.assertEqual(0, subprocess.call(['python', 'localDaemon.py', 'stop', '--pidfile=' + self.PID_FILE_PATH]))
            
        # Wait, that the pidfile will be deleted. If not break the test.
        self.assertTrue(wait_for(self._pid_file_deleted, 20), 'pidfile was not deleted in 20 seconds.')
        
    def _ensure_pid_file_is_deleted(self):
        try:
            if self._pid_file_created():
                os.remove(self.PID_FILE_PATH)
        except Exception:
            self.fail("Could not remove already existing pid_file under: %s" % self.PID_FILE_PATH)

    def _pid_file_created(self):
        return os.path.exists(self.PID_FILE_PATH)
    
    def _pid_file_deleted(self):
        return not os.path.exists(self.PID_FILE_PATH)

    def _given_repo_dir_from_test_resources(self, source_dir, target_dir):
        if os.path.exists(target_dir):
            shutil.rmtree(target_dir)
        
        if not source_dir.endswith('/'):
            source_dir += '/'
        
        shutil.copytree('src/test/resources/' + source_dir, target_dir)
        
def wait_for(expression_to_be_true, timeout_in_seconds):
    waited_seconds = 0 
    succeeded = expression_to_be_true()
    while (not succeeded) and (waited_seconds < timeout_in_seconds):
        time.sleep(1)
        waited_seconds += 1
        succeeded = expression_to_be_true()
    
    return succeeded
        
