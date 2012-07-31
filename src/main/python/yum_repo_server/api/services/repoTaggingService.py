import logging
import os
from yum_repo_server.api.services.repoConfigService import RepoConfigService
import lockfile
class IsNotAStaticRepoException(Exception):
  pass

class CouldNotLockTagsException(Exception):
  pass

class RepoTaggingService(object):
    config=RepoConfigService()


    def tagRepo(self, static_reponame, tag):
      repo = self.config.getStaticRepoDir(static_reponame)
      if not os.path.exists(repo):
        raise IsNotAStaticRepoException()
      tagpath = self.config.getTagsFileForStaticRepo(static_reponame)

      lock = lockfile.FileLock("static_reponame")
      while not lock.i_am_locking():
        try:
          lock.acquire(timeout=15) #wait 15sec max
        except LockTimeout:
          raise CouldNotLogTagsException()
      try:  
        fileHandle=open(tagpath,'a')
        fileHandle.write(tag)
        fileHandle.write('\n')
        fileHandle.close()
      finally:              
        lock.release()
      return "Tagged OK"

    def getTags(self,static_reponame):
       filepath=self.config.getTagsFileForStaticRepo(static_reponame)
       if not os.path.exists(filepath):
          return ""
       f = open(filepath, "r")
       tags = f.read()
       f.close()
       return tags

