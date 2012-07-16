import rpm
import os

from yum_repo_server import settings
from yum_repo_server.rpm.rpmfile import RpmFile
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from django.template import loader, Context
from django.http import Http404, HttpResponse

def rpm_info(rpm_path):
    if not os.path.exists(rpm_path):
        raise Http404("RPM %s not found" % rpm)
    
    if not os.path.isfile(rpm_path):
        raise Http404("%s is not a file")
    
    rpmFile = RpmFile(rpm_path)
    
    template = loader.select_template(['static/rpm_info.html'])
    context = Context({
        'rpm' : rpmFile,
    })
    return HttpResponse(template.render(context))

def rpm_info_static(request, rpm):
    config = RepoConfigService()
    rpm_path = os.path.join(config.getStaticRepoDir(), rpm) 
       
    if '../' in rpm:
        raise Http404('../ not allowed')
       
    return rpm_info(rpm_path)

def rpm_info_virtual(request, reponame, rpm):
    config = RepoConfigService()
    repoConfig = config.getConfig(reponame)
    rpm_path = os.path.join(config.getRepoDir(), repoConfig.destination[1:], rpm) 
    
    if '../' in rpm:
        raise Http404('../ not allowed')
    
    return rpm_info(rpm_path)

def virtual_repo_info(request, reponame):
    config = RepoConfigService()
    repoConfig = config.getConfig(reponame)
    
    template = loader.select_template(['static/virtual_repo_info.html'])
    context = Context({
        'reponame' : reponame,
        'config' : repoConfig,
        'staticRepos' : config.staticRepos,
    })
    return HttpResponse(template.render(context))

def error_page(request, error):
    template = loader.select_template(['error/' + error +'.htm'])
    response = HttpResponse(content = template.render(Context({})), status = error.__str__())
    return response
    
    
    
    