from yum_repo_server.settings import REPO_CONFIG
import os

def get_repo_dir():
    return REPO_CONFIG.get('REPO_DIR')

def _valid_repository_name(name):
    if '/' in name or ' ' in name:
        return False
    return True

def get_non_deletable_repositories(config_file=REPO_CONFIG['NON_DELETABLE_REPOSITORIES']):
    config_file=REPO_CONFIG['NON_DELETABLE_REPOSITORIES']
    repository_list = []
    
    if os.path.exists(config_file):
        repository_list = open(config_file,'r').readlines()
    
    repository_list = [entry.strip() for entry in repository_list]
    
    filtered_list = filter(_valid_repository_name, repository_list)
    
    return filtered_list

def is_mongo_update_enabled():
    return REPO_CONFIG['MONGO_UPDATE']

def get_mongo_update_host():
    return REPO_CONFIG['MONGO_UPDATE_HOST']

def get_mongo_update_prefix():
    return REPO_CONFIG['MONGO_UDPATE_PREFIX']