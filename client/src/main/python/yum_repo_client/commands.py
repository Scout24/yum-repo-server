import os
import re
from yum_repo_client.basiccommand import BasicCommand
from yum_repo_client.completer import StaticRepoCompleter, VirtualRepoCompleter, RepoTagCompleter, PathCompleter


class CreateStaticRepoCommand(BasicCommand):
    name = 'create'
    help_text = '<reponame>  : Creates a new empty repository on the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()

    def doRun(self, args):
        self.httpClient.createStaticRepo(args.reponame)


class DeleteRpmCommand(BasicCommand):
    name = 'deleterpm'
    help_text = '<reponame> <arch1>/<rpm1> ... <archN>/<rpmN> : Deletes rpms from the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()
        parser.add_argument('path', nargs='+', help='path to a rpm in the following form: <arch>/<filename>').completer = PathCompleter()

    def doRun(self, args):
        for rpm_file_name in args.path:
            self.httpClient.deleteSingleRpm(args.reponame, rpm_file_name)
            print '\t' + rpm_file_name


class DeleteStaticRepoCommand(BasicCommand):
    name = 'deletestatic'
    help_text = '<static_reponame> : Deletes the static repository. Virtual Repositories will still point to this not existing repository.'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()

    def doRun(self, args):
        self.httpClient.delete_static_repo(args.reponame)


class DeleteVirtualRepoCommand(BasicCommand):
    name = 'deletevirtual'
    help_text = '<virtual_reponame> : Deletes the virtual repository, but leaves the static repository untouched'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the virtual repository').completer = VirtualRepoCompleter()

    def doRun(self, args):
        self.httpClient.deleteVirtualRepo(args.reponame)


class GenerateMetadataCommand(BasicCommand):
    name = 'generatemetadata'
    help_text = '<reponame> : Generates Yum Metadata for this repository'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()

    def doRun(self, args):
        self.httpClient.generateMetadata(args.reponame)


class LinkToStaticCommand(BasicCommand):
    name = 'linktostatic'
    help_text = '<virtual_reponame> <static_reponame> : Creates a virtual repository linking to a static repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create').completer = VirtualRepoCompleter()
        parser.add_argument('static_reponame', help='name of the static repository to link to').completer = StaticRepoCompleter()

    def doRun(self, args):
        self.httpClient.createLinkToStaticRepo(args.virtual_reponame, args.static_reponame)


class LinkToVirtualCommand(BasicCommand):
    name = 'linktovirtual'
    help_text = '<virtual_reponame> <virtual_reponame> : Creates a virtual repository linking to another virtual repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create').completer = VirtualRepoCompleter()
        parser.add_argument('target_virtual_reponame', help='name of the virtual repository to link to').completer = VirtualRepoCompleter()

    def doRun(self, args):
        self.httpClient.createLinkToVirtualRepo(args.virtual_reponame, args.target_virtual_reponame)


class PropagateRpmCommand(BasicCommand):
    name = 'propagate'
    help_text = '<repo1> <arch>/<name> <repo2> : Propagates most recent matching rpm from repo1 to repo2'

    def add_arguments(self, parser):
        parser.add_argument('source_repo', help='name of the source repository').completer = StaticRepoCompleter()
        parser.add_argument('path', help='path of the rpm inside the repository').completer = PathCompleter()
        parser.add_argument('target_repo', help='name of the target repository').completer = StaticRepoCompleter()

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
        parser.add_argument('source_repo', help='name of the source repository').completer = StaticRepoCompleter()
        parser.add_argument('dest_repo', help='name of the destination repository').completer = StaticRepoCompleter()

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
    help_text = '[-name <regex>] [-showDestination true] : Query/filter virtual repositories'

    def add_arguments(self, parser):
        parser.add_argument('-name', help='regular expression to filter repository names')
        parser.add_argument('-showDestination', help='whether to show target repositories')

    def doRun(self, args):
        response = self.httpClient.queryVirtual(self.filterDefaults(args))
        print response.read()


class RedirectToExternalCommand(BasicCommand):
    name = 'redirectto'
    help_text = '<virtual_reponame> <redirect_url> : Creates a virtual repository redirecting to another external repository'

    def add_arguments(self, parser):
        parser.add_argument('virtual_reponame', help='name of the virtual repository to create').completer = VirtualRepoCompleter()
        parser.add_argument('redirect_url', help='url of the repository to link to')

    def doRun(self, args):
        self.httpClient.createVirtualRepo(args.virtual_reponame, args.redirect_url)


class GetTagListCommand(BasicCommand):
    name = 'taglist'
    help_text = '<repo> : Lists tags for <repo>'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()

    def doRun(self, args):
        response = self.httpClient.tagList(args.reponame)
        print response.read()


class AddTagCommand(BasicCommand):
    name = 'tag'
    help_text = '<repo> <tag> : Tags a repo with <tag>'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()
        parser.add_argument('tag', help='tag to set').completer = RepoTagCompleter()

    def doRun(self, args):
        self.httpClient.tagRepo(args.reponame, args.tag)


class DeleteTagCommand(BasicCommand):
    name = 'untag'
    help_text = '<repo> <tag> : Removes a <tag> from the repo'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()
        parser.add_argument('tag', help='tag to set').completer = RepoTagCompleter()

    def doRun(self, args):
        self.httpClient.untagRepo(args.reponame, args.tag)


class UploadRpmCommand(BasicCommand):
    name = 'uploadto'
    help_text = '<reponame> <rpm1> ... <rpmN> : Uploads rpms to a dedicated repository on the server'

    def add_arguments(self, parser):
        parser.add_argument('reponame', help='name of the static repository').completer = StaticRepoCompleter()
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