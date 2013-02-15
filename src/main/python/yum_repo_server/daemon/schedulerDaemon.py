#!/usr/bin/python
from yum_repo_server.settings import REPO_CONFIG
import os
from yum_repo_server.daemon.daemonRunnerWithStopNotification import DaemonRunnerWithStopNotification
from yum_repo_server.daemon.scheduler import MetaDataGenerationScheduler
import time
import logging
import logging.config
import sys

class SchedulerDaemon():
    metaDataGenerationScheduler = MetaDataGenerationScheduler()

    def __init__(self):
        pidFile = REPO_CONFIG.get('SCHEDULER_DAEMON_PIDFILE')

        self.stdout = sys.stdout
        self.stderr = sys.stderr
        self.pidfile_path = pidFile
        self.pidfile_timeout = 5

    def ensure_parent_path_exists(self, path):
        directory = os.path.dirname(path)
        if not os.path.isdir(directory):
            os.makedirs(directory)

    def run(self):
        logging.config.fileConfig(REPO_CONFIG['SCHEDULER_DAEMON_LOGGING_CONF'], disable_existing_loggers=False)
        self.metaDataGenerationScheduler.start()
        '''
            Do not end this thread, because the SIGTERM signal can only be caught from the main thread.
            See: http://docs.python.org/library/signal.html
        '''
        while True:
            time.sleep(20)

    def stop(self, signum, frame):
        self.metaDataGenerationScheduler.shutdown()
        exit(0)
        
def _set_pidfile_path_to_system_argument_if_given():
    pid_file_path = None
    
    if len(sys.argv) < 2:
        return
    
    for argument in sys.argv[2:]:
        if argument.startswith('--pidfile='):
            pid_file_path = argument.split('=')[1]
            break
            
    if pid_file_path is not None and pid_file_path.strip() != '':
        REPO_CONFIG['SCHEDULER_DAEMON_PIDFILE'] = pid_file_path

def callSchedulerDaemon():
    _set_pidfile_path_to_system_argument_if_given()
    schedulerDaemon = SchedulerDaemon()
    daemon_runner = DaemonRunnerWithStopNotification(schedulerDaemon)
    daemon_runner.do_action()

if __name__ == '__main__':
    callSchedulerDaemon()
