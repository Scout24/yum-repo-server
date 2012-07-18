from piston.handler import BaseHandler
from yum_repo_server.version import version
from django.http import HttpResponse
import httplib

class StatusHandler(BaseHandler):
    
    def read(self, request, *args, **kwargs):
        response = HttpResponse(status=httplib.OK, content_type='text/plain')
        response.content = version
        return response
