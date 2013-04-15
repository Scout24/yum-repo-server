import getpass
try:
    import json
except ImportError:
    import simplejson as json

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


class PathCompleter(object):

    def __call__(self, prefix, parsed_args, **kwargs):
        self.httpClient = HttpClient(hostname=parsed_args.hostname, port=parsed_args.port)
        response = self.httpClient.get_archs(parsed_args.reponame)
        archs = json.load(response)

        if not '/' in prefix:
            archs = [item['name'] for item in archs['items'] if item['name'].startswith(prefix) and item['name'] != 'repodata']
        else:
            prefix_parts = prefix.split('/')
            archs = [prefix_parts[0]]

        paths = []
        for arch in archs:
            response = self.httpClient.get_files(parsed_args.reponame, arch)
            files = json.load(response)
            for item in files['items']:
                path = arch + '/' + item['filename']
                if path.endswith('.rpm') and path.startswith(prefix):
                    paths.append(path)

        return paths
        #return ('noarch', 'items', str(archs['items']), )

