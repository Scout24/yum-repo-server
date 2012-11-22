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

    def read(self, request, repodir=''):
        if not self._valid(repodir):
            response = rc.BAD_REQUEST
            response.write("You are not allowed to look into %s" % repodir)
            return response
        
        is_virtual = repodir == 'virtual' 
        show_destination = is_virtual and 'showDestination' in request.GET and request.GET['showDestination'].lower() == 'true'
                
        
        root_dir = self.config.getStaticRepoDir()
        if is_virtual:
            root_dir=self.config.getVirtualRepoDir()
               
        repos = os.listdir(root_dir)
        repos = self.filter(request, repos)
        
        response = rc.ALL_OK       
        response.content=""
        
        for repo in repos:
            response.write(self._write_repo(repo, show_destination))
            response.write("\n")
                
        return response
    
    def _valid(self, repodir):
        if repodir in ["virtual", ""]:
            return True
        return False
    
    def _write_repo(self, repo, show_destination=False):
        
        if show_destination:
            destination = self.config.getConfig(repo).destination
            return '%s:%s' % (repo, destination)
            
        return repo

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