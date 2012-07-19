#!/usr/bin/env python
import os
import sys

if __name__ == "__main__":
    pathToProject = sys.path[0]
    pythonSrcPath = pathToProject + "/src/main/python"
    sys.path.insert(0, pythonSrcPath)
    sys.path.insert(0, pathToProject + "/client/src/main/python")

    # Override HttpRequest._load_post_and_files to handle our special legacy upload request via curl -d
    from yum_repo_server.api import upload
    from django import http
    http.HttpRequest._load_post_and_files = upload.handle_legacy_load_post_and_files
    
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "yum_repo_server.settings")
    os.environ.setdefault('YUM_REPO_SERVER_CONFIG', pathToProject + '/src/test/resources/server-config.yaml')
    os.environ.setdefault('YUM_REPO_SERVER_INSTALL_DIR', pythonSrcPath + "/yum_repo_server")
    # os.environ.setdefault('DJANGO_LIVE_TEST_SERVER_ADDRESS', '127.0.0.1:8081')
    
    if not os.path.exists('target/yum_repo_server/'):
        os.makedirs('target/yum_repo_server/')
    
    from django.core.management import execute_from_command_line

    execute_from_command_line(sys.argv)
