from yum_repo_server.api  import config
import os
import yaml
import rpm
import re
import subprocess
from yum_repo_server.settings import REPO_CONFIG
import logging
from yum_repo_server.api.services.rpmService import RpmService, compare_rpm_files

class RepoConfig(object):
    def __init__(self, data):
        self.data = data

    @property
    def destination(self):
        return self.data['destination']



class MetaDataGenerationConfig(object):
    def __init__(self, data):
        self.data = data

    def getMetaDataGenerationType(self):
        if self.data.has_key('generation_type'):
            return self.data['generation_type']

    def getMetaDataGenerationInterval(self):
        if self.data.has_key('generation_interval'):
            return self.data['generation_interval']

    def getMetaDataGenerationRpmMaxKeep(self):
        if self.data.has_key('rpm_max_keep'):
            return int(self.data['rpm_max_keep'])


class RepoNotFoundException(Exception):
    pass


class RepoConfigService(object):
    ALIAS_METADATA_FILENAME = "repo.yaml"
    METADATA_GENERATION_FILENAME = "metadata-generation.yaml"
    rpm_service = RpmService()

    def getTagsFileForStaticRepo(self, static_reponame):
        return os.path.join(self.getStaticRepoDir(static_reponame),'tags.txt')

    def getMetaDataGenerationFilePathRelativeToRepoDirByRepoName(self, static_reponame):
        return self.getStaticRepoDir() + '/' + static_reponame + '/' + self.METADATA_GENERATION_FILENAME

    def getConfig(self, virtual_reponame):
        config_filename = self.getVirtualRepoDir(virtual_reponame) + '/' + self.ALIAS_METADATA_FILENAME

        f = open(config_filename)
        try:
            return RepoConfig(yaml.load(f))
        finally:
            f.close()

    def getMetaDataGenerationConfig(self, static_reponame):
        config_filename = self.getStaticRepoDir(static_reponame) + '/' + self.METADATA_GENERATION_FILENAME
        f = open(config_filename)
        try:
            return MetaDataGenerationConfig(yaml.load(f))
        finally:
            f.close()


    def getRepoDir(self):
        return config.get_repo_dir()

    def getStaticRepoDir(self, reponame=''):
        static_path=config.get_repo_dir() + '/static/'
        if not os.path.exists(static_path): 
            os.makedirs(static_path)
        
        if '..' in reponame:
            raise ValueError('reponame is not allowed to contain ..')
            
        
        repo_path=static_path+reponame
        return repo_path

    def getVirtualRepoDir(self, reponame=''):
        virtual_path=config.get_repo_dir() + '/virtual/'
        if not os.path.exists(virtual_path):
            os.makedirs(virtual_path)
            
        if '..' in reponame:
            raise ValueError('reponame is not allowed to contain ..')
         
        
        repo_path=virtual_path+reponame
        return repo_path

    def createVirtualRepo(self, virtual_reponame, destination_relative_to_repodir):
        
        if re.match('^https?://.*', destination_relative_to_repodir):
            if destination_relative_to_repodir.endswith('/'):
                static_destination = destination_relative_to_repodir[:-1]
            else:
                static_destination = destination_relative_to_repodir
        else:
            destination = config.get_repo_dir() + '/' + destination_relative_to_repodir
            destination = self.removeTrailingSlashIfPresent(destination)
            logging.info("check if " + destination + " exists")
            if not os.path.exists(destination):
                raise RepoNotFoundException()
    
            if self.is_virtual_repository_path(destination):
                destination_reponame = destination_relative_to_repodir[len('virtual/'):]
                static_destination = self.getConfig(destination_reponame).destination
            else:#destination is static -> no need to check for repo.yaml just take destination as is
                static_destination = '/' + destination_relative_to_repodir

        self.create_virtual_repo_skeleton(virtual_reponame)
        return self.writeConfig(virtual_reponame, static_destination)

    def writeConfig(self, virtual_reponame, static_destination):
        config_filename = self.getVirtualRepoDir(virtual_reponame) + '/' + self.ALIAS_METADATA_FILENAME
        if os.path.exists(config_filename) and self.getConfig(virtual_reponame).destination == static_destination:
            link_already_existed = True
        else:
            new_config = dict(destination=static_destination)
            f = open(config_filename, 'w')
            try:
                yaml.safe_dump(new_config, f)
            finally:
                f.close()
            link_already_existed = False

        return dict(alias_to=static_destination, link_already_existed=link_already_existed)

    def create_virtual_repo_skeleton(self, virtual_reponame):
        path_to_virtual_repo = self.getVirtualRepoDir(virtual_reponame)
        if not os.path.exists(path_to_virtual_repo):
            os.makedirs(path_to_virtual_repo)

    def removeTrailingSlashIfPresent(self, destination):
        if destination.endswith("/"):
            return destination[:-1]
        else:
            return destination

    def is_virtual_repository_path(self, destination): #the path is relative to the project directory
        virtual_repo_path_prefix = config.get_repo_dir() + "/virtual"
        return destination.startswith(virtual_repo_path_prefix)

    def doCleanup(self, full_path_to_repo, rpm_max_keep):
        if type(rpm_max_keep) is not int:
            raise ValueError('rpm_max_keep must be an integer.')
        
        # if rpm_max_keep is zero or less, do not cleanup the repository.
        if rpm_max_keep < 1:
            return
        
        if os.path.exists(full_path_to_repo):
            arc_dirs = os.listdir(full_path_to_repo)
            for architecture in arc_dirs:
                path_to_arc_repo = full_path_to_repo + '/' + architecture
                if(os.path.isdir(path_to_arc_repo) and not architecture == 'repodata'):
                    rpms_to_delete = self._find_rpms_to_delete(os.listdir(path_to_arc_repo), rpm_max_keep)
                    self._delete_rpm_list(path_to_arc_repo, rpms_to_delete)
                    
    def _find_rpms_to_delete(self, file_name_list, rpm_max_keep):
        grouped_rpms = self.rpm_service.get_rpm_files_grouped_by_name(file_name_list)
        
        return self._check_group_for_rpms_to_delete(grouped_rpms, rpm_max_keep)

    def _check_group_for_rpms_to_delete(self, grouped_rpms, rpm_max_keep):
        list_of_rpms_to_delete = []
        for group_name in grouped_rpms.keys():
            list_of_rpms = grouped_rpms[group_name]
            list_of_rpms.sort(cmp=compare_rpm_files)
            list_of_rpms_to_delete.extend(list_of_rpms[0:len(list_of_rpms) - rpm_max_keep])
            
        return list_of_rpms_to_delete

    def _delete_rpm_list(self, path_to_arc_repo, rpms_to_delete):
        for rpm_file in rpms_to_delete:
            rpm_to_delete = path_to_arc_repo + '/' + rpm_file.file_name
            os.remove(rpm_to_delete)

    def getRepoLockFile(self, reponame):
        return os.path.join(self.getStaticRepoDir(reponame), '.lock')

    def doCreateRepo(self, full_path_to_repo, reponame):
        # Try to execute createrepo destination path
        repo_cache_dir = self.getRepoCacheDir(reponame)
        
        if not os.path.isdir(repo_cache_dir):
            os.makedirs(repo_cache_dir)
        
        command = "createrepo --update -v -d -c %s %s " % (repo_cache_dir, full_path_to_repo)
        lockfile = self.getRepoLockFile(reponame)
        try:
            open(lockfile, 'w').close() 
            pipe = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout = pipe.stdout.read()
            stderr = pipe.stderr.read()
            pipe.communicate()
            return_code = pipe.returncode
            if return_code > 0:
                logging.error(stderr)
        except Exception, ex:
            logging.error("exception occurred while calling createrepo : " + ex.message)
        finally:
            if os.path.exists(lockfile):
                os.remove(lockfile) 
            else:
                logging.error("lockfile "+lockfile+" does not exist.. skipping")
            
    def getRepoCacheDir(self, reponame = ''):
        return REPO_CONFIG.get('REPO_CACHE_DIR') + '/yum-repo-server/' + reponame
            
    @property
    def staticRepos(self):
        notStartWithDot = lambda s : not s.startswith('.')
        return sorted(filter(notStartWithDot, os.listdir(self.getStaticRepoDir())))
