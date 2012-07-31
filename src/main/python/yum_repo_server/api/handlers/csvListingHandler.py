import os
import re
import datetime
import time

from piston.handler import BaseHandler
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from piston.utils import rc
from yum_repo_server.api.services.repoTaggingService import RepoTaggingService

class CsvListingHandler(BaseHandler):

    config = RepoConfigService()

    def read(self, request,repodir=''):
        
        okToRead=["virtual",""]
        if repodir in okToRead:
            response = rc.ALL_OK       
            response.content=""
            rootDir = None
            if repodir=='':
                rootDir=self.config.getStaticRepoDir()
            if repodir=='virtual':
                rootDir=self.config.getVirtualRepoDir()
            if not rootDir :
                response=rc.BAD_REQUEST
                response.write("")
                return respose       
            if os.path.exists(rootDir) and os.path.isdir(rootDir):
                repos = os.listdir(rootDir)
                repos = self.filter(request, repos)
                for repo in repos:
                    response.write(repo)
                    response.write("\n")
        else:
            response = rc.BAD_REQUEST
            response.write("""
            You are not allowed to look into %s"""%repodir)
        return response

    def filter(self, request, repos):
        if 'name' in request.GET:
            pattern = re.compile(request.GET['name'])
            repos = filter(lambda d: pattern.match(d), repos)

        if 'tag' in request.GET:
            tags = set(request.GET['tag'].split(','))
            taggingService = RepoTaggingService()
            repos = filter(lambda d: len(tags.intersection(taggingService.getTags(d))) > 0, repos)

        if 'notag' in request.GET:
            forbiddentags = set(request.GET['notag'].split(','))
            taggingService = RepoTaggingService()
            repos = filter(lambda d: len(forbiddentags.intersection(taggingService.getTags(d))) == 0, repos)

        if 'older' in request.GET:
            pastTime = self.get_past_time(int(request.GET['older']))
            configService = RepoConfigService()
            repos = filter(lambda d: os.stat(configService.getStaticRepoDir(d)).st_mtime < pastTime, repos)

        if 'newer' in request.GET:
            pastTime = self.get_past_time(int(request.GET['newer']))
            configService = RepoConfigService()
            repos = filter(lambda d: os.stat(configService.getStaticRepoDir(d)).st_mtime > pastTime, repos)

        return repos

    def get_past_time(self, days):
        pastDay = datetime.datetime.now() - datetime.timedelta(days=days)
        return int(time.mktime(pastDay.timetuple()))