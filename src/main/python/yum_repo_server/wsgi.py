"""
WSGI config for django project.

This module contains the WSGI application used by Django's development server
and any production WSGI deployments. It should expose a module-level variable
named ``application``. Django's ``runserver`` and ``runfcgi`` commands discover
this application via the ``WSGI_APPLICATION`` setting.

Usually you will have the standard Django WSGI application here, but it also
might make sense to replace the whole Django WSGI application with a custom one
that later delegates to the Django one. For example, you could introduce WSGI
middleware here, or combine a Django application with an application of another
framework.

"""
import os
import sys

path = '/opt/yum-repo-server'
if path not in sys.path:
    sys.path.append(path)

os.environ['DJANGO_SETTINGS_MODULE'] = 'yum_repo_server.settings'

# Override HttpRequest._load_post_and_files to handle our special legacy upload request via curl -d
from yum_repo_server.api import upload
from django import http
http.HttpRequest._load_post_and_files = upload.handle_legacy_load_post_and_files

from django.core.handlers.wsgi import WSGIHandler

application = WSGIHandler()

