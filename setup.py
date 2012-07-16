#!/usr/bin/env python
import os
import shutil
from setuptools import setup
from distutils.command.clean import clean

class completeClean(clean):
    def run(self):
        if os.path.exists(self.build_base):
            shutil.rmtree(self.build_base)
            
        dist_dir = 'dist'
        if os.path.exists(dist_dir):
            shutil.rmtree(dist_dir)

def get_data_files_for(top_level_dir, base_dir):
    result = []
    for root, dirs, files in os.walk(top_level_dir):
        if '.svn' in root:
            continue
        
        target_files = []
        for f in files:
            target_files.append(os.path.join(root, f))
            
        if len(target_files) > 0:
            entry = (base_dir + root[len(top_level_dir):], target_files)
            result.append(entry)
            
    return result

setup(
    name = "yum-repo-server",
    version = "1.1",
    author = "Sebastian Herold, Kay Vogelgesang, Maximilien Riehl, Eric Ihrke",
    author_email = "sebastian.herold@immobilienscout24.de, kay.vogelgesang@immobilienscout24.de, maximilien.riehl@immobilienscout24.de, eric.ihrke@immobilienscout24.de",
    description = ("The yum-repo-server is a server that allows you to host and manage YUM repositories using a RESTful API."),
    license = "GNU GPL v3",
    keywords = "yum repository createrepo staging api",
    url = "https://github.com/is24-herold/yum-repo-server",
    packages=['yum_repo_server', 'yum_repo_server.api', 'yum_repo_server.daemon', 'yum_repo_server.rpm', 'yum_repo_server.api.handlers', 'yum_repo_server.api.services'],
    package_dir = {'' : 'src/main/python'},
    long_description=("The yum-repo-server is a server that allows you to host and manage YUM repositories using a RESTful API."),
    classifiers=[
        "Development Status :: 5 - Production/Stable",
        "Topic :: System :: Operating System",
        "Topic :: Software Development :: Build Tools",
        "License :: OSI Approved :: GNU General Public License v3 (GPLv3)",
    ],
    data_files=[('/etc/init.d', ['src/main/etc/init.d/yum_repo_daemon'])] +
        [('/etc/httpd/conf.d', ['src/main/etc/httpd/conf.d/yum-repo-server_wsgi_bindung.conf'])] +
        [('/opt/yum_repo_server', ['src/main/python/yum_repo_server/wsgi.py'])] +
        [('/opt/yum_repo_server/daemon', ['src/main/python/yum_repo_server/daemon/schedulerDaemon.py'])] +
        get_data_files_for('src/main/python/yum_repo_server/static', '/opt/yum_repo_server/static') +
        get_data_files_for('src/main/python/yum_repo_server/defaults', '/opt/yum_repo_server/defaults') +
        get_data_files_for('src/main/python/yum_repo_server/templates', '/opt/yum_repo_server/templates'),
    test_suite = "yum_repo_server.test.runtests.runtests",
    cmdclass={'clean' : completeClean},
)

