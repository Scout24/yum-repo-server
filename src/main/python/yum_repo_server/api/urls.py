from yum_repo_server.api.handlers.yumRepoHandler import YumRepoHandler
from yum_repo_server.api.handlers.yumRepoAliasHandler import YumRepoAliasHandler
from yum_repo_server.api.handlers.staticRepoHandler import StaticRepoHandler
from yum_repo_server.api.handlers.yumMetaDataHandler import YumMetaDataHandler
from yum_repo_server.api.handlers.virtualRepoHandler import VirtualRepoHandler
from yum_repo_server.api.handlers.virtualRepoConfigHandler import VirtualRepoConfigHandler
from yum_repo_server.api.handlers.rpmHandler import RpmHandler
from yum_repo_server.api.handlers.csvListingHandler import CsvListingHandler
from yum_repo_server.api.handlers.repositoryViewerHandler import RepositoryViewerHandler
from yum_repo_server.api.handlers.repoTaggingHandler import RepoTaggingHandler



from django.conf.urls.defaults import *

from piston.resource import Resource
from yum_repo_server import settings

createYumRepoResource = Resource(handler=YumRepoHandler)
staticRepoResource = Resource(handler=StaticRepoHandler)
createYumRepoAliasResource = Resource(handler=YumRepoAliasHandler)
createYumMetaDataResource = Resource(handler=YumMetaDataHandler)
deliverVirtualRepoResource = Resource(handler=VirtualRepoHandler)
repoConfigHandler = Resource(handler=VirtualRepoConfigHandler)
rpmResource = Resource(handler=RpmHandler)
repoCsvListingResource = Resource(handler=CsvListingHandler)
repositoryViewerResource = Resource(handler=RepositoryViewerHandler)
repoTaggingResource = Resource(handler=RepoTaggingHandler)

urlpatterns = patterns('',
    url(r'^(?P<repodir>[a-zA-Z0-9-_.]+)\.txt',repoCsvListingResource),
    url(r'^(?P<repodir>[a-zA-Z0-9-_.]+)/tags/(?P<tag>[\w\-_\.]*)$',repoTaggingResource),
    url(r'^(/?)$', createYumRepoResource),
    url(r'^virtual(/)?$', createYumRepoAliasResource),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-_.]+)\.json', repoConfigHandler),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-_.]+)/?\.html', 'yum_repo_server.api.views.virtual_repo_info'),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-_.]+)/(?P<rpm>[a-zA-Z0-9-_./]+)/info.html', 'yum_repo_server.api.views.rpm_info_virtual'),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-.]+)(?P<rpm>/[a-zA-Z0-9-_./]*)?$', deliverVirtualRepoResource),
    url(r'^(?P<reponame>[a-zA-Z0-9-_.]+)/?\.html', 'yum_repo_server.api.views.static_repo_info'),
    url(r'^(?P<reponame>[a-zA-Z0-9-_.]+)/repodata/?$', createYumMetaDataResource),
    url(r'^(?P<reponame>[a-zA-Z0-9-_.]+/?)$', staticRepoResource),
    url(r'^(?P<reponame>[a-zA-Z0-9-_.]+)/(?P<arch>[a-zA-Z0-9-_.]+)/(?P<rpm>[a-zA-Z0-9-_.]+)$', rpmResource),
    url(r'^(?P<rpm>[a-zA-Z0-9-_./]+)/info.html', 'yum_repo_server.api.views.rpm_info_static'),
    url(r'^(?P<path>.*)$', repositoryViewerResource),
)

# initialize temp dir for uploads
settings.initTempDir()
