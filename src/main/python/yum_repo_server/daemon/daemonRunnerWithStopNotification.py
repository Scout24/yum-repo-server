from daemon.runner import DaemonRunner, make_pidlockfile
from daemon.daemon import  DaemonContext
import signal
import pwd
import grp
from yum_repo_server.settings import REPO_CONFIG

class DaemonRunnerWithStopNotification(DaemonRunner):
    def __init__(self, app):
        self.parse_args()
        self.app = app
        
        signalHandlerMap = {signal.SIGTERM: self.app.stop}

        uid = pwd.getpwnam(REPO_CONFIG.get('DAEMON_USER')).pw_uid
        gid = grp.getgrnam(REPO_CONFIG.get('DAEMON_GROUP')).gr_gid
        self.daemon_context = DaemonContext(signal_map=signalHandlerMap, working_directory='.', uid=uid, gid=gid, umask=022)
        self.daemon_context.stdin = None
        self.daemon_context.stdout = self.app.stdout
        self.daemon_context.stderr = self.app.stderr

        self.pidfile = None
        if app.pidfile_path is not None:
            self.pidfile = make_pidlockfile(
                app.pidfile_path, app.pidfile_timeout)
        self.daemon_context.pidfile = self.pidfile
        
