import getpass
from yum_repo_client.repoclient import HttpClient


class StaticRepoCompleter(object):

    def __call__(self, prefix, parsed_args, **kwargs):
        self.httpClient = HttpClient(hostname=parsed_args.hostname, port=parsed_args.port)
        response = self.httpClient.queryStatic({})
        repo_str = response.read()
        return (reponame for reponame in repo_str.splitlines() if reponame.startswith(prefix))


class VirtualRepoCompleter(object):

    def __call__(self, prefix, parsed_args, **kwargs):
        self.httpClient = HttpClient(hostname=parsed_args.hostname, port=parsed_args.port)
        response = self.httpClient.queryVirtual({})
        repo_str = response.read()
        return (reponame for reponame in repo_str.splitlines() if reponame.startswith(prefix))


class RepoTagCompleter(object):

    def __call__(self, prefix, parsed_args, **kwargs):
        self.httpClient = HttpClient(hostname=parsed_args.hostname, port=parsed_args.port)
        response = self.httpClient.tagList(parsed_args.reponame)
        tag_str = response.read()
        return (tag for tag in tag_str.splitlines() if tag.startswith(prefix))


class UsernameCompleter(object):

    def __call__(self, prefix, parsed_args, **kwargs):
        user = getpass.getuser()
        if user.startswith(prefix):
            return (user, )
        else:
            return ()



