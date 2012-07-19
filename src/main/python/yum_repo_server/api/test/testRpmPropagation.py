from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase
import httplib

class TestRpmPropagation(BaseIntegrationTestCase):
    
    def test_propagate_rpm(self):
        first_repo = self.createNewRepoAndAssertValid()
        second_repo = self.createNewRepoAndAssertValid()
        
        destination_rpm_url = '/repo/%s/noarch/test-artifact-1.2-1.noarch.rpm' % second_repo
        location_field = self.live_server_url + destination_rpm_url
        
        self.repoclient().uploadRpm(first_repo, 'src/test/resources/test-artifact.rpm')
        
        response = self.repoclient().propagate_rpm(first_repo, 'noarch/test-artifact-1.2-1.noarch.rpm', second_repo)
        
        self.assertEquals(location_field, response.getheader('Location'))
        self.assertEquals(httplib.OK, self.helper.do_http_get(destination_rpm_url).status, 'Could not get the rpm via the expected destination path.')

    def test_rpm_propagation_to_a_non_existent_repository(self):
        first_repo = self.createNewRepoAndAssertValid()
        self.repoclient().uploadRpm(first_repo, 'src/test/resources/test-artifact.rpm')
        
        post_data = 'source=%s/noarch/test-artifact&destination=not-existing' % first_repo
        
        response = self.doHttpPost('/propagation/', post_data)
        self.assertEquals(httplib.BAD_REQUEST, response.status, 'expected statuscode 400 when destination does not exist.')
        
    def test_rpm_propagation_to_root_not_permitted(self):
        first_repo = self.createNewRepoAndAssertValid()
        self.repoclient().uploadRpm(first_repo, 'src/test/resources/test-artifact.rpm')
        
        post_data = 'source=%s/noarch/test-artifact-1.2-1.noarch.rpm&destination=/' % first_repo
        
        response = self.doHttpPost('/propagation/', post_data)
        self.assertEquals(httplib.BAD_REQUEST, response.status, 'expected statuscode 400 when destination does not exist.')

        post_data = 'source=%s/noarch/test-artifact-1.2-1.noarch.rpm&destination=.' % first_repo
        
        response = self.doHttpPost('/propagation/', post_data)
        self.assertEquals(httplib.BAD_REQUEST, response.status, 'expected statuscode 400 when destination does not exist.')
        
        
    def test_rpm_propagation_from_not_existing_repository(self):
        second_repo = self.createNewRepoAndAssertValid()
        post_data = 'source=not-existing/noarch/rpm-name&destination=%s' % second_repo
        
        response = self.doHttpPost('/propagation/', post_data)
        self.assertEquals(httplib.BAD_REQUEST, response.status, 'expected statuscode 400 when source repository does not exist.')
        
    def test_rpm_propagation_with_bad_formatted_source_string(self):
        second_repo = self.createNewRepoAndAssertValid()
        post_data = 'source=repo-name/rpm-name&destination=%s' % second_repo
        
        response = self.doHttpPost('/propagation/', post_data)
        self.assertEquals(httplib.BAD_REQUEST, response.status, 'expected statuscode 400 when source string is not correctly formatted.')
        
