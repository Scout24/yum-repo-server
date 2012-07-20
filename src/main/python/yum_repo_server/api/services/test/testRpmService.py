import unittest
from yum_repo_server.api.services.rpmService import RpmService,\
    create_rpm_file_object

class TestRpmService(unittest.TestCase):
    
    def test_get_latest_rpm(self):
        listed_rpms=(
            'tree-1.2-21.noarch.rpm',
            'tree-1.2-23.noarch.rpm',
            'tree-1.5-12.noarch.rpm'
            )
        
        latest_rpm = RpmService().get_latest_rpm_from_list('tree', listed_rpms)
        
        self.assertEquals('tree-1.5-12.noarch.rpm', latest_rpm)
    
    def test_get_latest_rpm_ignore_other_named_rpms(self):
        rpms=(
            'tree-1.2-21.noarch.rpm',
            'tree-1.2-23.noarch.rpm',
            'tree-2-2.5-12.noarch.rpm'
        )
        latest_rpm = RpmService().get_latest_rpm_from_list('tree', rpms)
        
        self.assertEquals('tree-1.2-23.noarch.rpm', latest_rpm)
        
    def test_get_latest_rpm_returns_none_when_rpm_not_found(self):
        rpms=(
            'tree-1.2-21.noarch.rpm',
            'tree-1.2-23.noarch.rpm',
        )
        latest_rpm = RpmService().get_latest_rpm_from_list('test-rpm', rpms)
        
        self.assertIsNone(latest_rpm)
        
    def test_get_rpm_files_grouped_by_name(self):
        rpms=(
            'tree-1.2-21.noarch.rpm',
            'tree-1.2-23.noarch.rpm',
            'tree-2-2.5-12.noarch.rpm'
        )
        rpm_groups = RpmService().get_rpm_files_grouped_by_name(rpms)
        
        self.assertEquals(2, len(rpm_groups))
        self.assertTrue(rpm_groups['tree'] != None)
        self.assertTrue(rpm_groups['tree-2'] != None)
    

class TestRpmFileObjectFactoryMethod(unittest.TestCase):
    
    def test_create_object(self):
        rpm_file = create_rpm_file_object('tree-1.5-12.noarch.rpm')
        self.assertEquals('tree-1.5-12.noarch.rpm', rpm_file.file_name)
        self.assertEquals('tree', rpm_file.name)
        self.assertEquals('1.5', rpm_file.version)
        self.assertEquals('12', rpm_file.release)
        self.assertEquals('noarch', rpm_file.architecture)
    
    def test_returns_none_on_invalid_rpm_names(self):
        invalid_rpms = ['.rpm',
                'noarch.rpm',
                'tree',
                'tree-1.2.noarch.rpm',
                'tree-1.2-23.rpm'
            ]
        
        invalid_names_not_found = []
        for invalid_name in invalid_rpms:
            try:
                if create_rpm_file_object(invalid_name) is not None:
                    invalid_names_not_found.append(invalid_name)
            except:
                invalid_names_not_found.append(invalid_name)
        
        self.assertEquals(0, len(invalid_names_not_found),
                          'the following rpms were not detected as invalid: %s ' % invalid_names_not_found)
        