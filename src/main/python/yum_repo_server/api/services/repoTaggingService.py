import logging
import os
from yum_repo_server.api.services.repoConfigService import RepoConfigService
import lockfile
class IsNotAStaticRepoException(Exception):
  pass

class CouldNotLockTagsException(Exception):
  pass

class NotFoundException(Exception):
  pass

class NoSuchTagException(Exception):
  pass

class RepoTaggingService(object):
    config=RepoConfigService()


    def tagRepo(self, static_reponame, tag):
      repo = self.config.getStaticRepoDir(static_reponame)
      tag_encoded=self.utf8Encode(tag)
      if not os.path.exists(repo):
        raise IsNotAStaticRepoException()
      tagpath = self.config.getTagsFileForStaticRepo(static_reponame)

      lock = lockfile.FileLock(tagpath)
      while not lock.i_am_locking():
        try:
          lock.acquire(timeout=15) #wait 15sec max
        except LockTimeout:
          raise CouldNotLogTagsException()
      try:  
        fileHandle=open(tagpath,'a')
        fileHandle.write(tag_encoded)
        fileHandle.write('\n')
        fileHandle.close()
      finally:              
        lock.release()
      return "Tagged OK"

    def utf8Encode(self,string):
      return string.encode('utf-8')

    def unTagRepo(self, static_reponame, tag):
      initialTags=self.getTags(static_reponame)
      tag_encoded=self.utf8Encode(tag)

      if not tag_encoded in initialTags:
        raise NoSuchTagException()

      initialTags.remove(tag_encoded)

      repo = self.config.getStaticRepoDir(static_reponame)
      if not os.path.exists(repo):
        raise IsNotAStaticRepoException()
      tagpath = self.config.getTagsFileForStaticRepo(static_reponame)

      lock = lockfile.FileLock(tagpath)
      while not lock.i_am_locking():
        try:
          lock.acquire(timeout=15) #wait 15sec max
        except LockTimeout:
          raise CouldNotLogTagsException()
      try:  
        fileHandle=open(tagpath,'w') #replace instead of appending
        for tag in initialTags:
          tag_encoded = self.utf8Encode(tag)
          fileHandle.write(tag_encoded)
          fileHandle.write('\n')
        fileHandle.close()
      finally:              
        lock.release()
      return "Untagged OK"

    def getTags(self,static_reponame):
       filepath=self.config.getTagsFileForStaticRepo(static_reponame)
       if not os.path.exists(self.config.getStaticRepoDir(static_reponame)):
          raise NotFoundException()
       if not os.path.exists(filepath):
          return set()
       f = open(filepath, "r")
       try:
           tags = set(f.read().split('\n'))
       finally:
           f.close()
       tags.discard('')
       return tags

    def getDefaultTags(self):
        tagFilePath = os.path.join(self.config.getRepoDir(), 'defaultTags.txt')
        if os.path.exists(tagFilePath):
            f = open(tagFilePath)
            try:
                content = f.read()
                tags = content.split('\n')
                tags = [tag.strip() for tag in tags]
                tagsSet = set(tags)
                tagsSet.remove('')
                return tagsSet
            finally:
                f.close()

        return set()
