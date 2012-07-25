import os

from yum_repo_server.api.handlers.yumRepoHandler import YumRepoHandler
from yum_repo_server.api.handlers.yumRepoAliasHandler import YumRepoAliasHandler
from yum_repo_server.api.handlers.uploadToYumRepoHandler import UploadToYumRepoHandler
from yum_repo_server.api.handlers.yumMetaDataHandler import YumMetaDataHandler
from yum_repo_server.api.handlers.virtualRepoHandler import VirtualRepoHandler
from yum_repo_server.api.handlers.virtualRepoConfigHandler import VirtualRepoConfigHandler
from yum_repo_server.api.handlers.staticRepoHandler import StaticRepoHandler
from yum_repo_server.api.handlers.csvListingHandler import CsvListingHandler



from django.conf.urls.defaults import *

from piston.resource import Resource
from yum_repo_server import settings

createYumRepoResource = Resource(handler=YumRepoHandler)
uploadToYumRepoHandler = Resource(handler=UploadToYumRepoHandler)
createYumRepoAliasResource = Resource(handler=YumRepoAliasHandler)
createYumMetaDataResource = Resource(handler=YumMetaDataHandler)
deliverVirtualRepoResource = Resource(handler=VirtualRepoHandler)
repoConfigHandler = Resource(handler=VirtualRepoConfigHandler)
staticRepoResource = Resource(handler=StaticRepoHandler)
repoCsvListingResource = Resource(handler=CsvListingHandler)

urlpatterns = patterns('',
    url(r'^(?P<repodir>[a-zA-Z0-9-.]+)\.txt',repoCsvListingResource),
    url(r'^(/?)$', createYumRepoResource),
    url(r'^virtual(/)?$', createYumRepoAliasResource),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-.]+)\.json', repoConfigHandler),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-.]+)/?\.html', 'yum_repo_server.api.views.virtual_repo_info'),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-.]+)/(?P<rpm>[a-zA-Z0-9-./]+)/info.html', 'yum_repo_server.api.views.rpm_info_virtual'),
    url(r'^virtual/(?P<reponame>[a-zA-Z0-9-.]+)(?P<rpm>/[a-zA-Z0-9-./]*)?$', deliverVirtualRepoResource),
    url(r'^(?P<reponame>[a-zA-Z0-9-.]+)/repodata/?$', createYumMetaDataResource),
    url(r'^(?P<reponame>[a-zA-Z0-9-.]+/?)$', uploadToYumRepoHandler),
    url(r'^(?P<rpm>[a-zA-Z0-9-./]+)/info.html', 'yum_repo_server.api.views.rpm_info_static'),
    url(r'^(?P<path>.*)$', staticRepoResource),
)

# initialize temp dir for uploads
settings.initTempDir()
