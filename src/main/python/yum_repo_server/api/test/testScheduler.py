import unittest
import shutil
import os
import time
from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
from yum_repo_server.daemon.scheduler import MetaDataGenerationScheduler
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.test import unique_repo_name


class TestScheduler(BaseIntegrationTestCase):
    configService = RepoConfigService()
    METADATA_SCHEDULED = """
generation_type: scheduled
generation_interval: 1
"""
    METADATA_MANUAL = """
generation_type: manual
"""
    
    def setUp(self):
        self.newRepoName = unique_repo_name()
        self.createdRepoDir = self.configService.getStaticRepoDir(self.newRepoName)
        self.mockedscheduler = self.get_mocked_scheduler()
    
    def tearDown(self):
        if os.path.exists(self.createdRepoDir):
            shutil.rmtree(self.createdRepoDir)
        
    def test_mockscheduler_schedules_update(self):
        self.create_dummy_repo_with_metadata_generation(None) #creates only repo, no metadata
        self.mockedscheduler.start() #should NOT queue a job for the repo
        jobs = self.mockedscheduler.sched.get_active_jobs()
        self.assertTrue(jobs["NOPATH"]=="update_program_config")
        
    def test_mockscheduler_does_not_care_about_manual_jobs(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_MANUAL)
        self.mockedscheduler.start() #should NOT queue a job for the repo (since is manual)
        jobs = self.mockedscheduler.sched.get_active_jobs()
        self.assertFalse(self.createdRepoDir in jobs)
        
    def test_mockscheduler_works_on_scheduled_jobs(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED)
        self.mockedscheduler.start() #should NOT queue a job for the repo (since is manual)
        jobs = self.mockedscheduler.sched.get_active_jobs()
        self.assertTrue(self.createdRepoDir in jobs)
        
    def test_mockscheduler_schedules_update_only_once_after_alternation(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED)
        self.mockedscheduler.start()
        
        self.unscheduled_counter=0
        self.add_job_counter=0
        def count_unschedule(reponame):
            self.unscheduled_counter += 1
            MetaDataGenerationScheduler.unschedule_by_reponame(self.mockedscheduler, reponame)
            
        def count_add_job(reponame):
            self.add_job_counter += 1
            MetaDataGenerationScheduler.add_job_for_repo(self.mockedscheduler, reponame)
        
        self.mockedscheduler.unschedule_by_reponame=count_unschedule
        self.mockedscheduler.add_job_for_repo=count_add_job
        
        # update timestamp of metadataFile
        metadata_file = self._get_metadata_generation_file_path(self.createdRepoDir)
        # ensure, that the timestamp will be changed.
        time.sleep(1)
        self._touch_file(metadata_file)
        
        self.mockedscheduler.update_program_config()
        self.mockedscheduler.update_program_config()
        self.mockedscheduler.update_program_config()
        
        self.assertEqual(1, self.unscheduled_counter, 'too many jobs were unscheduled.')
        self.assertEqual(1, self.add_job_counter, 'too many jobs were added.')
        
    def test_mockscheduler_works_on_scheduled_jobs_and_stops_them_when_repo_deleted(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED)
        self.mockedscheduler.start() #queues a job
        self.assertTrue(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        shutil.rmtree(self.createdRepoDir)
        self.mockedscheduler.update_program_config() #should remove the queued job since repo was deleted
        self.assertFalse(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        
    def test_mockscheduler_works_on_scheduled_jobs_and_stops_them_when_metadata_file_deleted(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED)
        self.mockedscheduler.start() #queues a job
        self.assertTrue(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        os.remove(self.configService.getMetaDataGenerationFilePathRelativeToRepoDirByRepoName(self.newRepoName))
        self.mockedscheduler.update_program_config() #should remove the queued job since file was deleted
        self.assertFalse(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        
        
    def test_mockscheduler_works_on_scheduled_jobs_and_stops_them_when_switching_to_manual(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED)
        self.mockedscheduler.start() #queues a job
        self.assertTrue(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        time.sleep(1) #THIS IS NECESSARY!! if not present, in some cases the timestamp does not change because the test is so fast, causing it to fail (no change = no update..)
        self.create_dummy_repo_with_metadata_generation(self.METADATA_MANUAL) #overwrite with manual        
        self.mockedscheduler.update_program_config() #should remove the queued job since was set to manual
        self.assertFalse(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        
    def test_mockscheduler_works_on_scheduled_jobs_after_switching_from_manual_to_scheduled(self):
        self.create_dummy_repo_with_metadata_generation(self.METADATA_MANUAL)
        self.mockedscheduler.start() #queues a job
        self.assertFalse(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        time.sleep(2) #THIS IS NECESSARY!! if not present, in some cases the timestamp does not change because the test is so fast, causing it to fail (no change = no update..)
        self.create_dummy_repo_with_metadata_generation(self.METADATA_SCHEDULED) #overwrite with scheduled        
        self.mockedscheduler.update_program_config() #should add a job since was set to scheduled
        self.assertTrue(self.createdRepoDir in self.mockedscheduler.sched.get_active_jobs())
        
    def get_mocked_scheduler(self):
        mockedscheduler = MetaDataGenerationScheduler(5)
        mockedscheduler.sched=MockedScheduler()
        return mockedscheduler
    
    def create_dummy_repo_with_metadata_generation(self,metadataContents):
        if not os.path.exists(self.createdRepoDir):
            os.makedirs(self.createdRepoDir)
        if not metadataContents:
            return
        filepath = self._get_metadata_generation_file_path(self.createdRepoDir)
        schedulingFile = open(filepath,'w')
        schedulingFile.write(metadataContents)
        schedulingFile.close()
    
    def _get_metadata_generation_file_path(self, repoDir):
        return repoDir+"/"+self.configService.METADATA_GENERATION_FILENAME
    
    def _touch_file(self, filename):
        handle = file(filename, 'a')
        try:
            os.utime(filename, None)
        finally:
            handle.close()

class MockedScheduler(object):
    activeJobs = {} #full_path_to_repo=funcName
    def add_interval_job(self,funcHook, seconds=5, args=None):
        if not args:
            fullpathtorepo="NOPATH" #magic constant to express that there is no path (because the update job takes no arguments)
                                    #necessary because None is not hashable and therefore cannot be used as a key
        else:
            fullpathtorepo = args[0]

        self.activeJobs[fullpathtorepo]=funcHook.__name__ 
        return fullpathtorepo #fake handle (full path to repo as a 1-element string list)
    def start(self):
        pass
    def shutdown(self):
        pass
    def get_active_jobs(self):
        return self.activeJobs
    def unschedule_job(self,jobHandle):
        del self.activeJobs[jobHandle]
    def add_cron_job(self, funcHook, second=0, hour=0, minute=0):
        pass

if __name__ == '__main__':
    unittest.main()