from django.conf.urls.defaults import *
from yum_repo_server import settings
from django.http import HttpResponseRedirect
import logging.config
from yum_repo_server.settings import REPO_CONFIG
from yum_repo_server.api.handlers.csvListingHandler import CsvListingHandler
from piston.resource import Resource
from yum_repo_server.status import StatusHandler
from yum_repo_server.api.handlers.rpmPropagationHandler import RpmPropagationHandler



# necessary to initialize the logger. Can't be done within logger script because of the daemon.
logging.config.fileConfig(REPO_CONFIG['SERVER_LOGGING_CONF'], disable_existing_loggers=False)

repoCsvListingResource = Resource(handler=CsvListingHandler)
statusHandlerResource = Resource(handler=StatusHandler)
rpmPropagationHandler = Resource(handler=RpmPropagationHandler)


# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'djangoTest.views.home', name='home'),
    # url(r'^djangoTest/', include('djangoTest.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    # url(r'^admin/', include(admin.site.urls)),
    
    url(r'^$', lambda x: HttpResponseRedirect('/repo/')),
    url(r'^repo.txt',repoCsvListingResource),
    url(r'^repo/', include('yum_repo_server.api.urls')),
    url(r'^propagation/?', rpmPropagationHandler),
    url(r'^status$', statusHandlerResource),
    url(r'^static/(?P<path>.*)$', 'yum_repo_server.static.serve', { 
            'document_root': settings.STATIC_ROOT,
        }),
    url(r'^error/(?P<error>\d+)\.htm$', 'yum_repo_server.api.views.error_page'),                   
)
