import os
import rpm

class RpmService(object):
    
    def get_latest_rpm(self, name, directory):
        return self.get_latest_rpm_from_list(name, os.listdir(directory))
        
    def get_latest_rpm_from_list(self, name, file_names):
        rpms_with_name = list(self._filter_rpm(file_names, self._that_starts_with(name)))
        
        rpm_groups = self.get_rpm_files_grouped_by_name(rpms_with_name)
        
        if not rpm_groups.has_key(name):
            return None
        
        rpm_files = rpm_groups[name]
        rpm_files.sort(cmp=compare_rpm_files)
        
        return rpm_files[-1].file_name
    
    def get_rpm_files_grouped_by_name(self, file_names):
        rpm_file_names = self._filter_rpm(file_names)
        rpm_groups = {}
        for rpm_file_name in rpm_file_names:
            rpm_file = create_rpm_file_object(rpm_file_name)

            if rpm_file is None:
                continue

            if not rpm_groups.has_key(rpm_file.name):
                rpm_groups[rpm_file.name] = [rpm_file]
            else:
                rpm_groups[rpm_file.name].append(rpm_file)

        return rpm_groups
    
    def _filter_rpm(self, file_names, additionalConditions=lambda file_name: True):
        return filter(lambda file_name: file_name.endswith('.rpm') and additionalConditions(file_name), file_names)
    
    def _that_starts_with(self, name):
        return lambda file_name: file_name.startswith(name)
    

class RpmFile():
    def __init__(self, file_name, name, version, release, architecture):
        self.file_name = file_name
        self.name = name
        self.version = version
        self.release =  release
        self.architecture = architecture
    
def compare_rpm_files(file1, file2):
    return rpm.labelCompare(('1', file1.version, file1.release), ('1', file2.version, file2.release))

def create_rpm_file_object(file_name):
    try:
        return _create_rpm_file_object(file_name)
    except:
        return None

def _create_rpm_file_object(file_name):
    if not file_name.endswith('.rpm') or file_name.count('-') < 2 or file_name.count('.') < 2:
        return None
    
    rpm_parts = file_name.rsplit('.', 2)

    rpm_name_parts = rpm_parts[0].rsplit('-', 2)
    return RpmFile(file_name, rpm_name_parts[0], rpm_name_parts[1], rpm_name_parts[2], rpm_parts[1])
