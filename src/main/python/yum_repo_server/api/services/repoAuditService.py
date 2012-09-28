from django.http import HttpRequest
import logging

class RepoAuditService(object):

  logger=logging.getLogger("audit")

  def log_action(self,function_description, request):
       username='<unauthenticated>'
       host='<unknown>'
       message='None'
       if 'REMOTE_USER' in request.META:
         username=request.META['REMOTE_USER']
       if 'REMOTE_HOST' in request.META:
         host=request.META['REMOTE_HOST']
       if 'YRS_MESSAGE' in request.POST and request.POST['YRS_MESSAGE'].strip():
         message=request.POST['YRS_MESSAGE']

       self.logger.info('User %s@%s %s. Reason : %s'%(username,host,function_description,message))
