import os
import shutil
import rpm
import sys
import time


class RpmException(Exception):
    pass


class RpmFileException (RpmException):
    pass


class RpmValidationException (RpmException):
    pass


class RpmFileHandler (object):
    VALID_OS = "linux"

    def __init__(self, absolute_file_name_to_rpm):
        self.rpm_file = RpmFile(absolute_file_name_to_rpm)

    def assert_valid(self):
        if self.rpm_file.binary:
            if self.rpm_file.os != RpmFileHandler.VALID_OS:
                raise RpmValidationException("RPM 'os' field does not match "
                                             "expectation: %s",
                                             self.rpm_file.os)
            if RpmFileHandler.VALID_OS not in self.rpm_file.platform:
                raise RpmValidationException("RPM 'platform' field does "
                                             "not match expectation: %s",
                                             self.rpm_file.platform)

    def build_canonical_rpm_file_name(self):
        return "%s-%s-%s.%s.rpm" % (self.rpm_file.name,
                                    self.rpm_file.version,
                                    self.rpm_file.release,
                                    self.rpm_file.arch if self.rpm_file.binary else "src")

    def rename_to_canonical_file_name(self):
        resulting_name = self.build_canonical_rpm_file_name()
        directory = os.path.dirname(self.rpm_file.file_name)
        absolute_file_name = os.path.join(directory, resulting_name)
        shutil.move(self.rpm_file.file_name, absolute_file_name)
        return absolute_file_name

    def move_to_canonical_name(self, destination):
        if self.rpm_file.binary:
            destination = os.path.join(destination, self.rpm_file.arch)
        else:
            destination = os.path.join(destination, "src")
        if not os.path.isdir(destination):
            os.mkdir(destination)
            
        resulting_name = os.path.join(destination, self.build_canonical_rpm_file_name())
        temporary_resulting_name = resulting_name + ".part"
        shutil.move(self.rpm_file.file_name,temporary_resulting_name)
        shutil.move(temporary_resulting_name,resulting_name)

class RpmFile (object):

    def __init__(self, file_name):
        if not os.path.exists(file_name):
            raise RpmFileException("File does not exist: %s", file_name)

        if not os.path.isfile(file_name):
            raise RpmFileException("Not a file: %s", file_name)

        self.file_name = file_name
        self._load_header_fields()

    def _load_header_fields(self):
        ts = rpm.TransactionSet()
        ts.setVSFlags((rpm._RPMVSF_NOSIGNATURES|rpm._RPMVSF_NODIGESTS))
        f = open(self.file_name, 'r')
        try:
            self.hdr = ts.hdrFromFdno(f)
            self.name = self.hdr['name']
            self.version = self.hdr['version']
            self.release = self.hdr['release']
            self.vendor = self.hdr['vendor']
            self.build_date = time.strftime("%d.%m.%Y %I:%M:%S", time.localtime(int(self.hdr['buildtime'])))
            self.build_host = self.hdr['buildhost']
            self.os = self.hdr['os']
            self.arch = self.hdr['arch']
            self.platform = self.hdr['platform']
            self.binary = self.hdr['sourcerpm'] != None and self.hdr['sourcerpm'] != "(none)"
            self.group = self.hdr['group']
            self.size = self.hdr['size']
            self.packager = self.hdr['packager']
            self.description = self.hdr['description']
            self.summary = self.hdr['summary']
            self.license = self.hdr['license']
            self.sourcerpm = self.hdr['sourcerpm']
            self.provides = sorted(self.hdr['provides'])
            self.requires = sorted(self.hdr['requires'])
            self.obsoletes = sorted(self.hdr['obsoletes'])
            self.conflicts = sorted(self.hdr['conflicts'])
        except rpm.error, e:
            raise RpmFileException("Not a rpm file: %s . Error: %s", self.file_name, str(e))
        finally:
            f.close()
