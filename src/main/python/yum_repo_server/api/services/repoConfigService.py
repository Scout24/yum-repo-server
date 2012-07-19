from yum_repo_server.api  import config
import os
import yaml
import rpm
import subprocess
from yum_repo_server.settings import REPO_CONFIG
import logging

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
        repo_path=static_path+reponame
        if not os.path.exists(static_path): 
            os.makedirs(static_path)
        return repo_path

    def getVirtualRepoDir(self, reponame=''):
        virtual_path=config.get_repo_dir() + '/virtual/'
        if not os.path.exists(virtual_path):
            os.makedirs(virtual_path)
        repo_path=virtual_path+reponame
        return repo_path

    def createVirtualRepo(self, virtual_reponame, destination_relative_to_repodir):
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

        path_to_virtual_repo = self.getVirtualRepoDir(virtual_reponame)
        self.create_virtual_repo_skeleton(path_to_virtual_repo)

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

    def create_virtual_repo_skeleton(self, path_to_virtual_repo):
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
                    rpm_files = os.listdir(path_to_arc_repo)
                    rpm_files = list(filter(lambda file_name: file_name.endswith('.rpm'), rpm_files))
                    if len(rpm_files) == 0:
                        continue
                    
                    rpm_group_with_obsolete_files = self._get_rpm_group_with_obsolete_files_by_file_name(rpm_files,
                        rpm_max_keep)
                    rpms_to_delete = self._check_dict_for_rpms_to_delete(rpm_group_with_obsolete_files, rpm_max_keep)
                    self._delete_rpm_list(path_to_arc_repo, rpms_to_delete)



    def _get_rpm_group_with_obsolete_files_by_file_name(self, rpm_file_names, rpm_max_keep):
        dict_obsolete_rpms = {}
        dict_rpm_groups = {}
        for rpm_file_name in rpm_file_names:
            rpm_group, rpm_group_infos = self._split_rpm_file_name(rpm_file_name)

            if rpm_group == None:
                continue

            if not dict_rpm_groups.has_key(rpm_group):
                dict_rpm_groups[rpm_group] = [rpm_group_infos]
            else:
                dict_rpm_groups[rpm_group].append(rpm_group_infos)
                if len(dict_rpm_groups[rpm_group]) > rpm_max_keep:
                    dict_obsolete_rpms[rpm_group] = dict_rpm_groups[rpm_group]

        return dict_obsolete_rpms

    def _check_dict_for_rpms_to_delete(self, dict_with_obsolete_rpms, rpm_max_keep):
        list_of_rpms_to_delete = []
        for rpm_group_name in dict_with_obsolete_rpms.keys():
            list_of_tuples = dict_with_obsolete_rpms[rpm_group_name]
            list_of_tuples.sort(cmp=self._compareTuples)
            list_of_rpms_to_delete.extend(list_of_tuples[0:len(list_of_tuples) - rpm_max_keep])
        return list_of_rpms_to_delete


    def _compareTuples(self, tuple1, tuple2):
        return rpm.labelCompare(('1', tuple1[0], tuple1[1]), ('1', tuple2[0], tuple2[1]))


    def _split_rpm_file_name(self, rpm_file):
        rpm_parts = rpm_file.rsplit('.', 2)
        if rpm_parts[2] != 'rpm':
            return None

        rpm_name_parts = rpm_parts[0].rsplit('-', 2)
        return rpm_name_parts[0], (rpm_name_parts[1], rpm_name_parts[2], rpm_file)

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
        except Exception as ex:
            logging.error("exception occurred while calling createrepo : " + ex.message)
        finally:
            if os.path.exists(lockfile):
                os.remove(lockfile) 
            else:
                logging.error("lockfile "+lockfile+" does not exist.. skipping")
            
    def getRepoCacheDir(self, reponame = ''):
        return REPO_CONFIG.get('REPO_CACHE_DIR') + '/yum-repo-server/' + reponame

    def _delete_rpm_list(self, path_to_arc_repo, rpms_to_delete):
        for rpm in rpms_to_delete:
            rpm_to_delete = path_to_arc_repo + '/' + rpm[2]
            os.remove(rpm_to_delete)
            
    @property
    def staticRepos(self):
        notStartWithDot = lambda s : not s.startswith('.')
        return sorted(filter(notStartWithDot, os.listdir(self.getStaticRepoDir())))
