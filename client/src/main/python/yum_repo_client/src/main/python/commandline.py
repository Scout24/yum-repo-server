#!/usr/bin/env python
import os
import sys
import argcomplete

import yaml
from argparse import ArgumentParser
from yum_repo_client.commands import *
from yum_repo_client.completer import UsernameCompleter


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

        argcomplete.autocomplete(self.parser)
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
                           help='username to use basic authentication. You will be prompted for the password.').completer = UsernameCompleter()
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
