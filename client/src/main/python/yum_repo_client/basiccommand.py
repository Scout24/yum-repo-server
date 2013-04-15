import getpass
from yum_repo_client.repoclient import HttpClient


class BasicCommand(object):
    def run(self, args):
        self.httpClient = HttpClient(hostname=args.hostname, port=args.port, message=args.message)
        if args.username is not None:
            self.httpClient.username = args.username
            self.httpClient.password = self._readPassword()
        return self.doRun(args)

    def filterDefaults(self, args):
        given_params = vars(args)
        params = {}
        for key in given_params:
            if key not in ['username', 'hostname', 'port'] and given_params[key] is not None:
                params[key] = str(given_params[key])

        return params

    def doRun(self, args):
        pass

    def _readPassword(self):
        return getpass.getpass()