
class Constants:
    # misc constants
    TESTREPO_PREFIX = "testrepo--"
    PATH_TO_STATIC_REPO_RELATIVE_TO_PROJECT_DIR = "/static/"
    PATH_TO_VIRTUAL_REPO_RELATIVE_TO_PROJECT_DIR = "/virtual/"

    HTTP_REPO = '/repo'
    HTTP_PATH_STATIC = HTTP_REPO
    HTTP_PATH_VIRTUAL = HTTP_REPO + '/virtual'
    TEST_RPM_FILE_LOC='src/test/resources/'
    TEST_RPM_FILE_NAME="test-artifact.rpm"
    TEST_RPM_DESTINATION_NAME = 'noarch/test-artifact-1.2-1.noarch.rpm'
    TEST_CORRUPT_RPM = 'server-config.yaml'
    TEST_SCHEDULER_YAML = 'src/test/resources/metadata-generation.yaml'

    GENERATE_REPO_DATA_POSTFIX = '/repodata'
    PRIMARY_XML = '/primary.xml.gz'
    
    #api message constants
    API_MSG_REGEX_MISMATCH = "The provided name is invalid. It must match this regular expression : ^[a-zA-Z0-9][a-zA-Z0-9_\-\.]*$"
    API_MSG_NO_POSTDATA = "POST data missing"
    API_MSG_NO_NAME_IN_POSTDATA = "The name attribute is missing"
    API_MSG_DUPLICATE_PREFIX = "The repository at target/static/"
    API_MSG_DUPLICATE_SUFFIX = " already exists."
    