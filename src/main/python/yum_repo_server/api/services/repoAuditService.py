from django.http import HttpRequest
import logging

class RepoAuditService(object):

  def log_action(self,function_description, request):
       username='<unauthenticated>'
       if 'REMOTE_USER' in request.META:
         username=request.META.REMOTE_USER
       logging.info('User %s %s'%(username,function_description))
