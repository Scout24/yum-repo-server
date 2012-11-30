from yum_repo_server.test.baseIntegrationTestCase import BaseIntegrationTestCase

class TestRepoPropagation(BaseIntegrationTestCase):

    def test_should_propagate_empty_repository(self):
        source_repo = self.createNewRepoAndAssertValid()
        destination_repo = self.createNewRepoAndAssertValid()

        self.repoclient().propagate_repo(source_repo, destination_repo)

    def test_should_propagate_repository_with_one_package(self):
        source_repo = self.createNewRepoAndAssertValid()
        destination_repo = self.createNewRepoAndAssertValid()

        self.upload_testfile(source_repo, "src/test/resources/test-artifact.rpm")

        self.repoclient().propagate_repo(source_repo, destination_repo)

        self.assert_repository_does_not_contain(source_repo, "noarch", "test-artifact-1.2-1.noarch.rpm")
        self.assert_repository_contains(destination_repo, "noarch", "test-artifact-1.2-1.noarch.rpm")

    def test_should_propagate_tagged_repository_with_one_package(self):
        source_repo = self.createNewRepoAndAssertValid()
        destination_repo = self.createNewRepoAndAssertValid()

        self.upload_testfile(source_repo, "src/test/resources/test-artifact.rpm")

        self.repoclient().tagRepo(source_repo, "simple-tag")
        self.repoclient().propagate_repo(source_repo, destination_repo)

        self.assert_repository_does_not_contain(source_repo, "noarch", "test-artifact-1.2-1.noarch.rpm")
        self.assert_repository_contains(destination_repo, "noarch", "test-artifact-1.2-1.noarch.rpm")

    def test_should_propagate_repository_even_if_metadata_has_been_generated_for_source_and_destination_repository(self):
        source_repo = self.createNewRepoAndAssertValid()
        destination_repo = self.createNewRepoAndAssertValid()

        self.upload_testfile(source_repo, "src/test/resources/test-artifact.rpm")
        self.repoclient().tagRepo(source_repo, "simple-tag")
        self.repoclient().generateMetadata(source_repo)
        self.repoclient().generateMetadata(destination_repo)

        self.repoclient().propagate_repo(source_repo, destination_repo)


    def test_should_propagate_repository_without_moving_generated_metadata(self):
        source_repo = self.createNewRepoAndAssertValid()
        destination_repo = self.createNewRepoAndAssertValid()

        self.upload_testfile(source_repo, "src/test/resources/test-artifact.rpm")
        self.repoclient().tagRepo(source_repo, "simple-tag")
        self.repoclient().generateMetadata(source_repo)


        self.repoclient().propagate_repo(source_repo, destination_repo)


        self.assert_repository_contains(source_repo, "repodata", "primary.xml.gz")
        self.assert_repository_does_not_contain(destination_repo, "repodata", "primary.xml.gz")
