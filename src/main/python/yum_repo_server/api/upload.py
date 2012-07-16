import time
from django.conf import settings
from django.http import QueryDict
from django.utils.datastructures import MultiValueDict
from django.core.files.uploadhandler import StopFutureHandlers


try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
    
def parse_legacy_upload(META, input_data, upload_handlers, encoding=None):
    #
    # Content-Length should contain the length of the body we are about
    # to receive.
    #
    # We have to import QueryDict down here to avoid a circular import.
    from django.http import QueryDict
    
    try:
        content_length = int(META.get('HTTP_CONTENT_LENGTH', META.get('CONTENT_LENGTH', 0)))
    except (ValueError, TypeError):
        # For now set it to 0; we'll try again later on down.
        content_length = 0

    # For compatibility with low-level network APIs (with 32-bit integers),
    # the chunk size should be < 2^31, but still divisible by 4.
    possible_sizes = [x.chunk_size for x in upload_handlers if x.chunk_size]
    chunk_size = min([2 ** 31 - 4] + possible_sizes)

    encoding = encoding or settings.DEFAULT_CHARSET
    
    post = QueryDict('', mutable=True)
    files = MultiValueDict()
    
    # HTTP spec says that Content-Length >= 0 is valid
    # handling content-length == 0 before continuing
    if content_length == 0:
        return QueryDict(MultiValueDict(), encoding=encoding), MultiValueDict()

    # See if the handler will want to take care of the parsing.
    # This allows overriding everything if somebody wants it.
    for handler in upload_handlers:
        result = handler.handle_raw_input(input_data,
                                          META,
                                          content_length,
                                          None,
                                          encoding)
        if result is not None:
            return result[0], result[1]

    field_name = 'rpmFile'
    tstamp = int(time.time() * 1000)
    file_name = 'upload-' + tstamp.__str__() + '.rpm'
    content_type = 'application/octet-stream'
    charset = None
  
    counters = [0] * len(upload_handlers)

    for handler in upload_handlers:
        try:
            handler.new_file(field_name, file_name,
                         content_type, content_length,
                         charset)
        except StopFutureHandlers:
            break

    while (1):
        chunk = input_data.read(chunk_size)
        if chunk:
            for i, handler in enumerate(upload_handlers):
                chunk_length = len(chunk)
                chunk = handler.receive_data_chunk(chunk,
                                                   counters[i])
                counters[i] += chunk_length
                if chunk is None:
                    # If the chunk received by the handler is None, then don't continue.
                    break
        else:
            break
            
    for i, handler in enumerate(upload_handlers):
        file_obj = handler.file_complete(counters[i])
        if file_obj:
            # If it returns a file object, then set the files dict.
            files.appendlist(field_name, file_obj)
            break
            
    for handler in upload_handlers:
        retval = handler.upload_complete()
        if retval:
            break

    return post, files

    
def handle_legacy_load_post_and_files(self):
    # Populates self._post and self._files
    if self.method != 'POST':
        self._post, self._files = QueryDict('', encoding=self._encoding), MultiValueDict()
        return
    if self._read_started and not hasattr(self, '_raw_post_data'):
        self._mark_post_parse_error()
        return

    if self.META.get('CONTENT_TYPE', '').startswith('multipart'):
        if hasattr(self, '_raw_post_data'):
            # Use already read data
            data = StringIO(self._raw_post_data)
        else:
            data = self
        try:
            self._post, self._files = self.parse_file_upload(self.META, data)
        except:
            # An error occured while parsing POST data.  Since when
            # formatting the error the request handler might access
            # self.POST, set self._post and self._file to prevent
            # attempts to parse POST data again.
            # Mark that an error occured.  This allows self.__repr__ to
            # be explicit about it instead of simply representing an
            # empty POST
            self._mark_post_parse_error()
            raise
    elif self.META.get('CONTENT_TYPE', '') == 'application/x-www-form-urlencoded':
        if hasattr(self, '_raw_post_data'):
            # Use already read data
            data = StringIO(self._raw_post_data)
        else:
            data = self
        self._post, self._files = MultiValueDict(), MultiValueDict()
        self._post, self._files = parse_legacy_upload(self.META, data, self.upload_handlers, self.encoding)
    else:
        self._post, self._files = QueryDict(self.raw_post_data, encoding=self._encoding), MultiValueDict()
