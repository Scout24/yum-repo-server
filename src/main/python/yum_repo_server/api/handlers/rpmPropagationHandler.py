from piston.handler import BaseHandler
from piston.utils import rc
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoAuditService import RepoAuditService
import os
import shutil
from yum_repo_server.api.services.rpmService import create_rpm_file_object,\
    RpmService

class RpmPropagationHandler(BaseHandler):
    config = RepoConfigService()
    audit = RepoAuditService()
    
    def create(self, request):
        data = request.POST
        
        if not data.has_key('source') or not data.has_key('destination'):
            return self._build_bad_response('source and destination attribute must be set.')
        
        try:             
            source_repo, source_arch, rpm = data['source'].split('/')
        except ValueError:
            return self._build_bad_response('source format is not valid. (repo_name/architecture/rpm-name)')
        
        source_repo_path = self.config.getStaticRepoDir(source_repo)
        
        if not os.path.exists(source_repo_path):
            return self._build_bad_response('source repository does not exist.')

        destination = data['destination']
        if '/' in destination or str(destination).startswith('.'):
            return self._build_bad_response('destination must not contain slashes.')

        destination_repo_path = self.config.getStaticRepoDir(destination)
        if not os.path.exists(destination_repo_path):
            return self._build_bad_response('destination repository does not exist')
        
        rpm = self._determine_rpm_file_name(os.path.join(source_repo_path, source_arch), rpm)
        
        if not rpm:
            return self._build_bad_response('rpm_file could not be found.')
        
        source_rpm_path = os.path.join(source_repo_path, source_arch, rpm)
        destination_rpm_path = os.path.join(destination_repo_path, source_arch, rpm)
        if not os.path.exists(source_rpm_path):
            return self._build_bad_response('rpm_file could not be found.')
        
        destination_rpm_parent_dir = os.path.dirname(destination_rpm_path)
        if not os.path.exists(destination_rpm_parent_dir):
            os.mkdir(destination_rpm_parent_dir)

        self.audit.log_action("propagated rpm %s/%s from %s to %s"%(source_arch, rpm, source_repo,destination),request)
        shutil.move(source_rpm_path, destination_rpm_path)
        
        resp = rc.CREATED
        resp['Location'] = os.path.join('/repo', data['destination'], source_arch, rpm)
        
        return resp

    def _determine_rpm_file_name(self, directory, rpm):
        if create_rpm_file_object(rpm) is not None:
            return rpm
        
        return RpmService().get_latest_rpm(rpm, directory)

    def _build_bad_response(self, msg):
        resp = rc.BAD_REQUEST
        resp.content = msg
        return resp

