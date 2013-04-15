#!/usr/bin/env python
import os
import httplib
from dummy_thread import exit
import pycurl
import sys
import getpass
import string
import base64
import re
import urllib

import yaml
from argparse import ArgumentParser


class RepoException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)


class HttpClient(object):
    USER_AGENT = 'repoclient/1.0'

    username = None
    password = None
    message = None

    def __init__(self, hostname, port, message=None):
        self.hostname = hostname
        self.port = port
        self.message = message

    def queryStatic(self, params):
        urlparams = urllib.urlencode(params)
        response = self.doHttpGet('/repo.txt?%s' % urlparams)
        self.assertResponse(response, httplib.OK)
        return response

    def queryVirtual(self, params):
        urlparams = urllib.urlencode(params)
        response = self.doHttpGet('/repo/virtual.txt?' + urlparams)
        self.assertResponse(response, httplib.OK)
        return response

    def createStaticRepo(self, reponame):
        response = self.doHttpPost('/repo/', "name=" + reponame)
        self.assertResponse(response, httplib.CREATED)
        return response

    def delete_static_repo(self, reponame):
        response = self.doHttpDelete('/repo/%s' % reponame)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def untagRepo(self, reponame, tag):
        response = self.doHttpDelete('/repo/' + reponame + '/tags/' + tag)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def tagRepo(self, reponame, tag):
        response = self.doHttpPost('/repo/' + reponame + '/tags/', "tag=" + tag)
        self.assertResponse(response, httplib.CREATED)
        return response

    def tagList(self, reponame):
        response = self.doHttpGet('/repo/' + reponame + '/tags/')
        self.assertResponse(response, httplib.OK)
        return response

    def uploadRpm(self, reponame, rpm_file_name):
        c = pycurl.Curl()
        c.setopt(c.POST, 1)
        url = "http://%s:%d/repo/%s/" % (self.hostname, self.port, reponame)
        c.setopt(c.URL, url)
        c.setopt(c.HTTPPOST, [("rpmFile", (c.FORM_FILE, rpm_file_name))])
        c.setopt(pycurl.HTTPHEADER, ['User-Agent: ' + self.USER_AGENT])
        if self.username is not None:
            c.setopt(pycurl.HTTPAUTH, pycurl.HTTPAUTH_BASIC)
            c.setopt(pycurl.USERPWD, '%s:%s' % (self.username, self.password))

        c.perform()
        returncode = c.getinfo(pycurl.HTTP_CODE)
        c.close()

        if returncode != httplib.CREATED:
            raise RepoException("Upload failed.")

    def deleteSingleRpm(self, reponame, rpm_file_name):
        response = self.doHttpDelete('/repo/' + reponame + '/' + rpm_file_name)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def generateMetadata(self, reponame):
        response = self.doHttpPost('/repo/' + reponame + '/repodata')
        self.assertResponse(response, httplib.CREATED)
        return response

    def createVirtualRepo(self, virtual_reponame, destination_reponame):
        post_data = 'name=' + virtual_reponame + "&destination=" + destination_reponame
        response = self.doHttpPost('/repo/virtual/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def createLinkToVirtualRepo(self, virtual_reponame, destination_virtual_reponame):
        return self.createVirtualRepo(virtual_reponame, 'virtual/' + destination_virtual_reponame)

    def createLinkToStaticRepo(self, virtual_reponame, static_reponame):
        return self.createVirtualRepo(virtual_reponame, 'static/' + static_reponame)

    def deleteVirtualRepo(self, virtual_reponame):
        response = self.doHttpDelete('/repo/virtual/' + virtual_reponame)
        self.assertResponse(response, httplib.NO_CONTENT)
        return response

    def propagate_rpm(self, fromrepo, rpm_arch_slash_name, torepo):
        post_data = 'source=' + fromrepo + "/" + rpm_arch_slash_name + "&destination=" + torepo
        response = self.doHttpPost('/propagation/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def propagate_repo(self, source_repository, destination_repository):
        post_data = 'source=' + source_repository + "&destination=" + destination_repository
        response = self.doHttpPost('/repo-propagation/', post_data)
        self.assertResponse(response, httplib.CREATED)
        return response

    def doHttpPost(self, extPath, postdata='', headers=None):
        if postdata and self.message:
            postdata += '&YRS_MESSAGE=' + str(self.message)
        if not headers:
            headers = {}
        headers['User-Agent'] = self.USER_AGENT

        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth
        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.connect()
            httpServ.request('POST', extPath, postdata, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit()

    def doHttpDelete(self, extPath):
        headers = {'User-Agent': self.USER_AGENT}

        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth

        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.request('DELETE', extPath, None, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit()

    def doHttpGet(self, extPath):
        headers = {'User-Agent': self.USER_AGENT}

        if self.username is not None:
            auth = 'Basic ' + string.strip(base64.encodestring(self.username + ':' + self.password))
            headers['Authorization'] = auth

        try:
            httpServ = httplib.HTTPConnection(self.hostname, self.port)
            httpServ.request('GET', extPath, None, headers)
            response = httpServ.getresponse()
            return response
        except httplib.HTTPException:
            print "ERROR! Looks like the server is not running on " + self.hostname
            exit()

    def assertResponse(self, response, expectedStatus):
        if response.status != expectedStatus:
            raise RepoException(
                "ERROR: Got unexpected status code %(status)d .Expected %(expected)d Response: %(content)s" % {
                    "status": response.status,
                    "expected": expectedStatus,
                    "content": response.read()})


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


class CreateStaticRepoCommand(BasicCommand):
    name = 'create'
    help_text = '<reponame>  : Creates a new empty repository on the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')

    def doRun(self, args):
        self.httpClient.createStaticRepo(args.reponame)


class DeleteRpmCommand(BasicCommand):
    name = 'deleterpm'
    help_text = '<reponame> <arch1>/<rpm1> ... <archN>/<rpmN> : Deletes rpms from the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')
        parser.add_argument('path', nargs='+', help='path to a rpm in the following form: <arch>/<filename>')

    def doRun(self, args):
        for rpm_file_name in args.path:
            self.httpClient.deleteSingleRpm(args.reponame, rpm_file_name)
            print '\t' + rpm_file_name


class DeleteStaticRepoCommand(BasicCommand):
    name = 'deletestatic'
    help_text = '<static_reponame> : Deletes the static repository. Virtual Repositories will still point to this not existing repository.'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')

    def doRun(self, args):
        self.httpClient.delete_static_repo(args.reponame)


class DeleteVirtualRepoCommand(BasicCommand):
    name = 'deletevirtual'
    help_text = '<virtual_reponame> : Deletes the virtual repository, but leaves the static repository untouched'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the virtual repository')

    def doRun(self, args):
        self.httpClient.deleteVirtualRepo(args.reponame)


class GenerateMetadataCommand(BasicCommand):
    name = 'generatemetadata'
    help_text = '<reponame> : Generates Yum Metadata for this repository'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')

    def doRun(self, args):
        self.httpClient.generateMetadata(args.reponame)


class LinkToStaticCommand(BasicCommand):
    name = 'linktostatic'
    help_text = '<virtual_reponame> <static_reponame> : Creates a virtual repository linking to a static repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create')
        parser.add_argument('static_reponame', help='name of the static repository to link to')

    def doRun(self, args):
        self.httpClient.createLinkToStaticRepo(args.virtual_reponame, args.static_reponame)


class LinkToVirtualCommand(BasicCommand):
    name = 'linktovirtual'
    help_text = '<virtual_reponame> <virtual_reponame> : Creates a virtual repository linking to another virtual repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create')
        parser.add_argument('target_virtual_reponame', help='name of the virtual repository to link to')

    def doRun(self, args):
        self.httpClient.createLinkToVirtualRepo(args.virtual_reponame, args.target_virtual_reponame)


class PropagateRpmCommand(BasicCommand):
    name = 'propagate'
    help_text = '<repo1> <arch>/<name> <repo2> : Propagates most recent matching rpm from repo1 to repo2'

    def add_arguments(self, parser):
        parser.add_argument('source_repo', help='name of the source repository')
        parser.add_argument('path', help='path of the rpm inside the repository')
        parser.add_argument('target_repo', help='name of the target repository')

    def doRun(self, args):
        pattern = re.compile('[\w]+/[\w]+')
        match = pattern.match(args.path)
        if match is None:
            print "ERROR : Your input, '" + args.path + """' did not match the required pattern.
        It should look like this : <rpm_arch>/<rpm_name>"""
            return 1

        response = self.httpClient.propagate_rpm(args.source_repo, args.path, args.target_repo)
        print "INFO: move to location: " + response.getheader("Location")


class PropagateRepoCommand(BasicCommand):
    name = 'propagaterepo'
    help_text = '<source_repository> <destination_repository> : Propagates all packages in source_repository to destination_repository'

    def add_arguments(self, parser):
        parser.add_argument('source_repo', help='name of the source repository')
        parser.add_argument('dest_repo', help='name of the destination repository')

    def doRun(self, args):
        print "INFO: propagating repository {0} to {1}".format(args.source_repo, args.dest_repo)
        self.httpClient.propagate_repo(args.source_repo, args.dest_repo)
        print "INFO: finished propagation."


class QueryStaticReposCommand(BasicCommand):
    name = 'querystatic'
    help_text = '[-name <regex>] [-tag <tag1,tagN>] [-notag <tag1,tagN>] [-newer <days>] [-older <days>] : Query/filter static repositories'

    def add_arguments(self, parser):
        parser.add_argument('-name', help='regular expression to filter repository names')
        parser.add_argument('-tag', help='tags the repositories should have')
        parser.add_argument('-notag', help='tags the repositories should not have')
        parser.add_argument('-newer', type=int, help='maximal age of the repository in days')
        parser.add_argument('-older', type=int, help='minimal age of the repository in days')

    def doRun(self, args):
        response = self.httpClient.queryStatic(self.filterDefaults(args))
        print response.read()


class QueryVirtualReposCommand(BasicCommand):
    name = 'queryvirtual'
    help_text = '[-name <regex>] [-newer <days>] [-older <days>] [-showDestination true] : Query/filter virtual repositories'

    def add_arguments(self, parser):
        parser.add_argument('-name', help='regular expression to filter repository names')
        parser.add_argument('-newer', type=int, help='maximal age of the repository in days')
        parser.add_argument('-older', type=int, help='minimal age of the repository in days')
        parser.add_argument('-showDestination', help='whether to show target repositories')

    def doRun(self, args):
        response = self.httpClient.queryVirtual(self.filterDefaults(args))
        print response.read()


class RedirectToExternalCommand(BasicCommand):
    name = 'redirectto'
    help_text = '<virtual_reponame> <redirect_url> : Creates a virtual repository redirecting to another external repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create')
        parser.add_argument('redirect_url', help='url of the repository to link to')

    def doRun(self, args):
        self.httpClient.createVirtualRepo(args.virtual_reponame, args.redirect_url)


class GetTagListCommand(BasicCommand):
    name = 'taglist'
    help_text = '<repo> : Lists tags for <repo>'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')

    def doRun(self, args):
        response = self.httpClient.tagList(args.reponame)
        print response.read()


class AddTagCommand(BasicCommand):
    name = 'tag'
    help_text = '<repo> <tag> : Tags a repo with <tag>'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')
        parser.add_argument('tag', help='tag to set')

    def doRun(self, args):
        self.httpClient.tagRepo(args.reponame, args.tag)


class DeleteTagCommand(BasicCommand):
    name = 'untag'
    help_text = '<repo> <tag> : Removes a <tag> from the repo'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')
        parser.add_argument('tag', help='tag to set')

    def doRun(self, args):
        self.httpClient.untagRepo(args.reponame, args.tag)


class UploadRpmCommand(BasicCommand):
    name = 'uploadto'
    help_text = '<reponame> <rpm1> ... <rpmN> : Uploads rpms to a dedicated repository on the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository')
        parser.add_argument('path', nargs='+', help='local path to a rpm on disk')

    def doRun(self, args):
        error = False
        for rpm_file_name in args.path:
            if not os.path.exists(rpm_file_name):
                print 'ERROR: %s not found' % rpm_file_name
                error = True
                continue
            self.httpClient.uploadRpm(args.reponame, rpm_file_name)
            print '\t'

        if error:
            return 1


class CommandLineClient(object):
    operations = {}
    arguments = []
    options = {}
    commands = [CreateStaticRepoCommand(), DeleteRpmCommand(), DeleteStaticRepoCommand(),
                DeleteVirtualRepoCommand(), GenerateMetadataCommand(), LinkToStaticCommand(),
                LinkToVirtualCommand(), PropagateRpmCommand(), PropagateRepoCommand(),
                QueryStaticReposCommand(), QueryVirtualReposCommand(), RedirectToExternalCommand(),
                GetTagListCommand(), AddTagCommand(), DeleteTagCommand(), UploadRpmCommand()]

    def __init__(self, arguments):
        self.defaultConfig = DefaultConfigLoader()

        self.parser = ArgumentParser()
        self._add_default_arguments(self.parser)

        subparsers = self.parser.add_subparsers(title='Commands', help='commands')

        for command in self.commands:
            subparser = subparsers.add_parser(command.name, help=command.help_text)
            command.add_arguments(subparser)
            self._add_default_arguments(subparser)
            subparser.set_defaults(func=command.run)

        self.arguments = self.parser.parse_args(arguments[1:])

    def execute(self):
        if self.arguments.hostname is None:
            print "ERROR: you must specify a hostname using --hostname=<hostname>"
            return self.showHelp()
        if self.arguments.port is None:
            print "ERROR: you must specify a server port using --port=<port>"
            return self.showHelp()

        try:
            res = self.arguments.func(self.arguments)
            if res:
                return res
            else:
                return 0
        except Exception, e:
            print e
            return 1

    def showHelp(self):
        self.parser.print_help()
        return 1

    def _add_default_arguments(self, parser):
        group = parser.add_argument_group('global settings')
        group.add_argument('-s', '--hostname', default=self.defaultConfig.hostname,
                           help='hostname of the yum repo server. Default: set by /etc/yum-repo-client.yaml')
        group.add_argument('-p', '--port', type=int, default=self.defaultConfig.port,
                           help='port of the yum repo server. Default: 80 unless set by /etc/yum-repo-client.yaml')
        group.add_argument('-u', '--username',
                           help='username to use basic authentication. You will be prompted for the password.')
        group.add_argument('-m', '--message',
                           help='adds a justification to your request. It will be visible in the audit.')


class OptionParsingException(Exception): pass


class DefaultConfigLoader(object):
    """
        Extracts known options (properties of this class) and removes them from the
        given argument list. Options are marked through two hyphen at the beginning
        of the argument.
    """

    def __init__(self):
        if 'YUM_REPO_CLIENT_CONFIG' in os.environ:
            config_filename = os.environ['YUM_REPO_CLIENT_CONFIG']
        else:
            config_filename = '/etc/yum-repo-client.yaml'

        if os.path.exists(config_filename):
            f = open(config_filename)
            try:
                config = yaml.load(f)
                self.hostname = config['DEFAULT_HOST']
                self.port = config['DEFAULT_PORT']
            finally:
                f.close()

    hostname = None
    port = None


def mainMethod():
    exitCode = CommandLineClient(sys.argv).execute()
    sys.exit(exitCode)


if __name__ == '__main__':
    mainMethod()

