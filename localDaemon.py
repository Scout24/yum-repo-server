#!/usr/bin/env python

import os
import sys
import pwd
import grp

if __name__ == "__main__":
    pathToProject = sys.path[0]
    pythonSrcPath = pathToProject + "/src/main/python"
    sys.path.append(pythonSrcPath)

    os.environ.setdefault('YUM_REPO_SERVER_CONFIG', pathToProject + '/src/test/resources/server-config.yaml')
    os.environ.setdefault('YUM_REPO_SERVER_INSTALL_DIR', pythonSrcPath)

    from yum_repo_server.settings import REPO_CONFIG
    REPO_CONFIG['DAEMON_USER'] = pwd.getpwuid(os.getuid()).pw_name
    REPO_CONFIG['DAEMON_GROUP'] = grp.getgrgid(os.getgid()).gr_name
    # ensure, that the pidfile-path is absolute.
    REPO_CONFIG['SCHEDULER_DAEMON_PIDFILE'] = os.path.abspath(REPO_CONFIG['SCHEDULER_DAEMON_PIDFILE'])
    
    if not os.path.exists('target/yum_repo_server/'):
        os.makedirs('target/yum_repo_server/')
    
    from yum_repo_server.daemon.schedulerDaemon import callSchedulerDaemon
    callSchedulerDaemon()