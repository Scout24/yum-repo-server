import   os
import shutil
from apscheduler.scheduler import Scheduler
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.daemon.jobMonitorer import JobMonitorer
import traceback
import logging


class MetaDataGenerationScheduler():
    def __init__(self, updateIntervalSeconds=30):
        self.interval = updateIntervalSeconds
        config = {'apscheduler.daemonic': False}
        self.sched = Scheduler(config)
        # initialize these per instance.
        self.repo_timestamps = {}
        self.jobs = {}


    repo_timestamps = {}  #dictionary with jobName (=reponame) : last scheduler modification timestamp (float)
    jobs = {} #dictionary with jobName (=reponame) : jobHandle

    configService = RepoConfigService()
    static_root_dir = configService.getStaticRepoDir()
    sched = None
    interval = None

    def start(self):
        self.update_program_config() #read configs, schedule jobs

        # schedule an update as a job
        self.sched.add_interval_job(self.update_program_config, seconds=self.interval)
        
        # schedule cleanup cache
        self.sched.add_cron_job(self.cleanupCacheDir, hour = 23, minute = 17, second = 20)

        self.sched.start()
        
    def createrepo_with_optional_cleanup_job(self, *argList):
        monitor = JobMonitorer()
        monitor.job_starts()
        repoDir = argList[0]
        reponame = argList[1]
        rpm_max_keep = argList[2]
        didCleanUp=False
        try:
            if rpm_max_keep != None:
                didCleanUp=True
                self.configService.doCleanup(repoDir, rpm_max_keep)
                logging.info("job RpmCleanup on "+reponame+" took "+str(monitor.get_execution_time_until_now_seconds())+" seconds")
            self.configService.doCreateRepo(repoDir, reponame)
            monitor.job_finishes()
            logging.info(monitor.get_pretty_job_summary("createrepo on "+reponame+" (cleanup included : "+str(didCleanUp)+")"))
        except Exception, ex:
            logging.error(traceback.format_exc())

    def update_program_config(self):
        updatedJobs = 0
        addedJobs = 0
        removedJobs = 0
        
        list_of_static_dirs = os.listdir(self.static_root_dir)
        self.remove_jobs_where_repo_deleted(list_of_static_dirs)
        for static_dir in list_of_static_dirs:
            file_path = self.configService.getMetaDataGenerationFilePathRelativeToRepoDirByRepoName(static_dir)
            if not os.path.exists(file_path):
                if self.repo_timestamps.has_key(static_dir):
                    logging.debug("unschedule because file does not exist")
                    self.unschedule_by_reponame(static_dir)
                    del self.repo_timestamps[static_dir] #repo is unmanaged now, check back later
                    removedJobs+=1
                continue
            
            if not static_dir in self.repo_timestamps:
                logging.debug("new repo found..")
                addedJobs+=1
                self.repo_timestamps[static_dir] = self.determine_last_modification_time(
                    file_path) #make an entry so we know we processed the repo + remember modification timestamp
                self.add_job_for_repo(static_dir)
            else: # we already processed the repo because its in the dictionary
                logging.debug("check for updates in repo config...")
                if self.is_more_recent_metadata_generation_file_than(static_dir, self.repo_timestamps[static_dir]):
                    logging.debug("update job for repo " + static_dir)
                    updatedJobs+=1
                    self.repo_timestamps[static_dir] = self.determine_last_modification_time(file_path)
                    self.unschedule_by_reponame(static_dir)
                    self.add_job_for_repo(static_dir)
        logging.info("update_program_config finished -- updated %s jobs, added %s jobs, removed %s jobs"%(updatedJobs,addedJobs,removedJobs))

    def remove_jobs_where_repo_deleted(self, list_of_existing_repos):
        removed_repos = set(self.repo_timestamps.keys()) - set(list_of_existing_repos)
        for repo in removed_repos:
            self.unschedule_by_reponame(repo)

    def determine_last_modification_time(self, file_path):
        statbuf = os.stat(file_path)
        return statbuf.st_mtime #float representing the last modification timestamp

    def unschedule_by_reponame(self, reponame):
        if reponame in self.jobs:
            self.sched.unschedule_job(self.jobs[reponame])
            del self.jobs[reponame] #remove the job from the job-handle dictionary..

    def is_more_recent_metadata_generation_file_than(self, repodir, past_timestamp):
        file_path = self.configService.getMetaDataGenerationFilePathRelativeToRepoDirByRepoName(repodir)
        actual_timestamp = self.determine_last_modification_time(file_path)
        if actual_timestamp > past_timestamp:
            return True
        else:
            return False
        
    def cleanupCacheDir(self):
        cleanupCacheMonitor = JobMonitorer()
        cleanupCacheMonitor.job_starts()
        logging.info('Start cache cleanup ...')
        cleanupDir = self.configService.getRepoCacheDir()
        
        try:
            for reponame in os.listdir(cleanupDir):
                if reponame.startswith('.'):
                    continue
                
                # check for cache dirs of already deleted repos 
                absoluteDir = os.path.join(cleanupDir, reponame)
                if os.path.isdir(absoluteDir):
                    if not os.path.exists(self.configService.getStaticRepoDir(reponame)):
                        shutil.rmtree(absoluteDir)
                        continue
                    
                           
                lockfile = self.configService.getRepoLockFile(reponame)
                if not os.path.exists(lockfile):
                    shutil.rmtree(absoluteDir)
        except Exception, ex:
            logging.error("Exception in CleanupCacheDir : "+str(ex))                    
        finally:
            cleanupCacheMonitor.job_finishes()
            logging.info(cleanupCacheMonitor.get_pretty_job_summary("CleanupCacheDir"))        

    def add_job_for_repo(self, repo_dir):
        metaDataConfig = self.configService.getMetaDataGenerationConfig(repo_dir)
        if not metaDataConfig:
            return #exit silently without adding a job
        generation_type = metaDataConfig.getMetaDataGenerationType()
        if generation_type == 'manual': return #exit silently
        #if we get here, we know its "scheduled"
        generation_interval = metaDataConfig.getMetaDataGenerationInterval()
        generation_interval = int(generation_interval)
        rpm_max_keep = metaDataConfig.getMetaDataGenerationRpmMaxKeep()
        full_path_to_repo = self.configService.getStaticRepoDir(repo_dir)
        argList = [full_path_to_repo, repo_dir, rpm_max_keep]
        addedJob = self.sched.add_interval_job(self.createrepo_with_optional_cleanup_job, seconds=generation_interval, args=argList)
        self.jobs[repo_dir] = addedJob

    def shutdown(self):
        self.sched.shutdown()
