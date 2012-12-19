"""
Modification of django.views.static.py:

Views and functions for serving static files. These are only to be used
during development, and SHOULD NOT be used in a production setting.
"""

import mimetypes
import os
import posixpath
import re
import urllib
import time

from operator import attrgetter
from django.http import Http404, HttpResponse, HttpResponseRedirect, HttpResponseNotModified
from django.template import loader, Context
from django.utils.http import http_date, parse_http_date
from yum_repo_server.api.services.repoConfigService import RepoConfigService
from yum_repo_server.api.services.repoTaggingService import RepoTaggingService
from yum_repo_server.settings import REPO_CONFIG

RANGE_PATTERN = '^bytes=(\d{1,9})-(\d{0,9})$'

class ParentDirType(object):
    NONE=0
    STATIC=1
    VIRTUAL=2


def serve_file(fullpath, request):
    # Respect the If-Modified-Since header.
    statobj = os.stat(fullpath)
    mimetype, encoding = mimetypes.guess_type(fullpath)
    mimetype = mimetype or 'application/octet-stream'
    if not was_modified_since(request.META.get('HTTP_IF_MODIFIED_SINCE'),
        statobj.st_mtime, statobj.st_size):
        return HttpResponseNotModified(mimetype=mimetype)

    if request.META.get('HTTP_RANGE'):
        range_description = request.META.get('HTTP_RANGE')
        range_match = re.match(RANGE_PATTERN, range_description)
        if not range_match:
            return HttpResponse(content='http range "%s" does not match "%s".' % (range_description, RANGE_PATTERN), status=416)

        start_byte = int(range_match.group(1))
        last_byte = range_match.group(2)
        if start_byte < 0:
            return HttpResponse(content='http start byte %d is not allowed to be less than 0.' % start_byte, status=416)
        if not last_byte:
            last_byte = statobj.st_size - 1
        else:
            last_byte = int(last_byte)
        if start_byte > last_byte:
            return HttpResponse(content='http start byte %d is not allowed to be bigger than last byte %d.' % (start_byte, last_byte), status=416)
        if start_byte >= statobj.st_size or last_byte >= statobj.st_size:
            return HttpResponse(content='http start byte %d or last byte %d is bigger than file size %d.' % (start_byte, last_byte, statobj.st_size), status=416)

        content_length = last_byte - start_byte + 1
        with open(fullpath, 'rb') as file_obj:
            file_obj.seek(start_byte)
            response = HttpResponse(file_obj.read(content_length), mimetype=mimetype, status=206)

        response['Accept-Ranges'] = 'bytes'
        response['Content-Range'] = 'bytes %d-%d/%d' % (start_byte, last_byte, statobj.st_size)
    else:
        content_length = statobj.st_size
        if 'XSENDFILE' in REPO_CONFIG and REPO_CONFIG['XSENDFILE'] is 'true':
            response = HttpResponse(mimetype=mimetype)
            response['X-Sendfile'] = fullpath
            response['Used-Sendfile'] = 'true'
        else:
            response = HttpResponse(open(fullpath, 'rb').read(), mimetype=mimetype)
            response['Used-Sendfile'] = 'false'


    response["Last-Modified"] = http_date(statobj.st_mtime)
    response["Content-Length"] = content_length
    if encoding:
        response["Content-Encoding"] = encoding
    return response


def serve(request, path, document_root=None, show_indexes=False, add_virtual = False, parent_dir_type = ParentDirType.NONE):
    """
    Serve static files below a given point in the directory structure.

    To use, put a URL pattern such as::

        (r'^(?P<path>.*)$', 'django.views.static.serve', {'document_root' : '/path/to/my/files/'})

    in your URLconf. You must provide the ``document_root`` param. You may
    also set ``show_indexes`` to ``True`` if you'd like to serve a basic index
    of the directory.  This index view will use the template hardcoded below,
    but if you'd like to override it, you can create a template called
    ``static/directory_index.html``.
    """
    path = posixpath.normpath(urllib.unquote(path))
    path = path.lstrip('/')
    newpath = ''
    for part in path.split('/'):
        if not part:
            # Strip empty path components.
            continue
        drive, part = os.path.splitdrive(part)
        head, part = os.path.split(part)
        if part in (os.curdir, os.pardir):
            # Strip '.' and '..' in path.
            continue
        newpath = os.path.join(newpath, part).replace('\\', '/')
    if newpath and path != newpath:
        return HttpResponseRedirect(newpath)
    fullpath = os.path.join(document_root, newpath)
    if os.path.isdir(fullpath):
        if show_indexes:
            full_request_path = request.get_full_path()
            if not full_request_path.endswith('/'):
                return HttpResponseRedirect(full_request_path + '/')
            return directory_index(path=newpath, fullpath=fullpath, add_virtual=add_virtual, parent_dir_type=parent_dir_type)
        raise Http404("Directory indexes are not allowed here.")
    if not os.path.exists(fullpath):
        raise Http404('"%s" does not exist' % fullpath)

    return serve_file(fullpath, request)

KWOWN_FILE_TYPES = ['.rpm', '.xml.gz', '.xml', '.sqlite.bz2']

class FileInfo(object):

    taggingService = RepoTaggingService()
    filename = 'foo'
    fullpath = 'bar'
    isDir = False
    tags = []
    
    def __init__(self, filename, parentDir, parentDirType = ParentDirType.NONE):
        self.filename = filename
        self.fullpath = os.path.join(parentDir, filename)
        self.isDir = os.path.isdir(self.fullpath)
        if self.isDir:
            self.filename += '/'
        else :
            self.size = os.path.getsize(self.fullpath)
        
        self.mtime = time.strftime("%d.%m.%Y %H:%M:%S", time.localtime(os.path.getctime(self.fullpath)))
        self.mimetype = self.getMimeType()
        self.isFile = not self.isDir
        if parentDirType == ParentDirType.STATIC and filename != '':
            self.tags = self.taggingService.getTags(filename)

        self.hasInfo = parentDirType == ParentDirType.STATIC or parentDirType == ParentDirType.VIRTUAL
            
    def getMimeType(self):
        if self.isDir:
            return 'folder'
        else:
            for fileType in KWOWN_FILE_TYPES:
                if self.filename.endswith(fileType):
                    return fileType[1:]
            
            return 'unknown'

def directory_index(path, fullpath, add_virtual = False, parent_dir_type = ParentDirType.NONE):
    t = loader.select_template(['static/directory_index.html', 'static/directory_index'])
    unsorted_files = []
    for f in os.listdir(fullpath):
        if not f.startswith('.'):
            unsorted_files.append(FileInfo(f, fullpath, parent_dir_type))
            
    if add_virtual:
        virtual_repodir = RepoConfigService().getVirtualRepoDir()
        repodir=os.path.join(virtual_repodir,os.path.pardir)
        relative_virtualdir=os.path.basename(os.path.normpath(virtual_repodir))
        file_info = FileInfo(relative_virtualdir, repodir)
        if os.path.exists(file_info.fullpath):
            unsorted_files.append(file_info)
        
    files = sorted(unsorted_files, key = attrgetter('isFile', 'filename'))
        
    c = Context({
        'directory' : path,
        'file_list' : files,
        'file_count': len(files),
    })
    return HttpResponse(t.render(c))

def was_modified_since(header=None, mtime=0, size=0):
    """
    Was something modified since the user last downloaded it?

    header
      This is the value of the If-Modified-Since header.  If this is None,
      I'll just return True.

    mtime
      This is the modification time of the item we're talking about.

    size
      This is the size of the item we're talking about.
    """
    try:
        if header is None:
            raise ValueError
        matches = re.match(r"^([^;]+)(; length=([0-9]+))?$", header,
                           re.IGNORECASE)
        header_mtime = parse_http_date(matches.group(1))
        header_len = matches.group(3)
        if header_len and int(header_len) != size:
            raise ValueError
        if mtime > header_mtime:
            raise ValueError
    except (AttributeError, ValueError, OverflowError):
        return True
    return False
