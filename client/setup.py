#!/usr/bin/env python
import os
import sys
import shutil
from distutils.command.clean import clean

from setuptools import setup


commands = {}

'''
    Need to do this, in order to use the TeamcityTestRunner without installing it first. 
    Add the runner only conditionally, because we don't have it while packaging the repo_client
    as a rpm.
'''
if os.path.exists('src/main/python/teamcity_test_runner_extension'):
    sys.path.insert(0, 'src/main/python')

    from teamcity_test_runner_extension.teamcityTestRunner import TeamcityTestRunnerCommand

    commands['test'] = TeamcityTestRunnerCommand


class completeClean(clean):
    def run(self):
        if os.path.exists(self.build_base):
            shutil.rmtree(self.build_base)

        dist_dir = 'dist'
        if os.path.exists(dist_dir):
            shutil.rmtree(dist_dir)

        egg_dir = 'src/main/python/yum_repo_client.egg-info'
        if os.path.exists(egg_dir):
            shutil.rmtree(egg_dir)


commands['clean'] = completeClean


setup(
    name="yum-repo-client",
    version="1.2",
    author="Sebastian Herold, Kay Vogelgesang, Maximilien Riehl, Eric Ihrke",
    author_email="sebastian.herold@immobilienscout24.de, kay.vogelgesang@immobilienscout24.de, maximilien.riehl@immobilienscout24.de, eric.ihrke@immobilienscout24.de",
    description=("The yum-repo-client is a command line interface for the yum repo server."),
    license="GNU GPL v3",
    keywords="yum repository createrepo staging api command line",
    url="https://github.com/is24-herold/yum-repo-server",
    packages=['yum_repo_client'],
    long_description=("The yum-repo-client is a command line interface for the yum repo server."),
    package_dir={'': 'src/main/python'},
    classifiers=[
        "Development Status :: 5 - Production/Stable",
        "Topic :: System :: Operating System",
        "Topic :: Software Development :: Build Tools",
        "License :: OSI Approved :: GNU General Public License v3 (GPLv3)",
    ],
    test_suite="yum_repo_client",
    cmdclass=commands,
    data_files=[('/etc/bash_completion.d', ['src/main/bash-completion/yum-repo-client.bash'])],
    entry_points={
        'console_scripts': [
            'repoclient = yum_repo_client.commandline:mainMethod',
        ],
    },
    install_requires=['pycurl', 'argcomplete', 'simplejson'],
)
