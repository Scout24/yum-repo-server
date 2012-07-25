import yum_repo_server
import os
import time
import sys
import uuid
from yum_repo_server.rpm.rpmfile import RpmFileHandler
from yum_repo_server.rpm.rpmfile import RpmFileException
from yum_repo_server.rpm.rpmfile import RpmValidationException
from yum_repo_server.static import serve
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from piston.handler import BaseHandler
from piston.utils import rc
import logging

class UploadToYumRepoHandler(BaseHandler):
    
    repoConfigService = RepoConfigService()
    TEMP_DIR = yum_repo_server.settings.REPO_CONFIG['TEMP_DIR']

    def create(self, request, reponame):
        tempFilename = self._save_as_temp_file(request)
        logging.info('Saved uploaded rpm to ' + tempFilename)
        
        repoPath = self.repoConfigService.getStaticRepoDir(reponame)
        try:
            rpmFileHandler = RpmFileHandler(tempFilename)
            rpmFileHandler.assert_valid()
            rpmFileHandler.move_to_canonical_name(repoPath)
        except (RpmFileException, RpmValidationException) as e:
            sys.stderr.write("ERROR validating %s: %s\n" % (tempFilename, str(e)))
            if os.path.exists(tempFilename):
                os.remove(tempFilename)
            return rc.BAD_REQUEST

        return rc.CREATED
    
    def read(self, request, reponame):
        return serve(request, reponame, self.repoConfigService.getStaticRepoDir(), True)

    def _save_as_temp_file(self, request):
        request_file = request.FILES["rpmFile"]

        if not os.path.isdir(self.TEMP_DIR):
            os.makedirs(self.TEMP_DIR)

        path = self.TEMP_DIR + '/' + self._get_time_stamp() + '.' + str(uuid.uuid4())

        targetFile = open(path, 'wb+')
        try:
            for chunk in request_file.chunks():
                targetFile.write(chunk)
        finally:
            targetFile.close()

        return path

    def _get_time_stamp(self):
        return str(int(round(time.time() * 1000)))

