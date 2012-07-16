from yum_repo_server import settings

def get_repo_dir():
    return settings.REPO_CONFIG.get('REPO_DIR')

