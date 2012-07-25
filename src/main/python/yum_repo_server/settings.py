# Django settings for yum_repo_server project.
import os
import os.path
import yaml
import tempfile

from django.core.files.uploadedfile import UploadedFile

DEBUG = True
TEMPLATE_DEBUG = DEBUG

ADMINS = (
    # ('Your Name', 'your_email@example.com'),
)

MANAGERS = ADMINS

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3', # Add 'postgresql_psycopg2', 'mysql', 'sqlite3' or 'oracle'.
        'NAME': '',                      # Or path to database file if using sqlite3.
        'USER': '',                      # Not used with sqlite3.
        'PASSWORD': '',                  # Not used with sqlite3.
        'HOST': '',                      # Set to empty string for localhost. Not used with sqlite3.
        'PORT': '',                      # Set to empty string for default. Not used with sqlite3.
    }
}

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# On Unix systems, a value of None will cause Django to use the same
# timezone as the operating system.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'Europe/Berlin'

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# If you set this to False, Django will not format dates, numbers and
# calendars according to the current locale.
USE_L10N = True

# If you set this to False, Django will not use timezone-aware datetimes.
USE_TZ = True

# Absolute filesystem path to the directory that will hold user-uploaded files.
# Example: "/home/media/media.lawrence.com/media/"
MEDIA_ROOT = ''

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash.
# Examples: "http://media.lawrence.com/media/", "http://example.com/media/"
MEDIA_URL = ''

# URL prefix for static files.
# Example: "http://media.lawrence.com/static/"
STATIC_URL = '/static-ignored/'

# Additional locations of static files
STATICFILES_DIRS = (
    # Put strings here, like "/home/html/static" or "C:/www/django/static".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
)

# List of finder classes that know how to find static files in
# various locations.
STATICFILES_FINDERS = (
    'django.contrib.staticfiles.finders.FileSystemFinder',
    'django.contrib.staticfiles.finders.AppDirectoriesFinder',
#    'django.contrib.staticfiles.finders.DefaultStorageFinder',
)

# Make this unique, and don't share it with anybody.
SECRET_KEY = 't2)_%g+4qu4i)1c(2*a&amp;xn94_vn&amp;p2!^esb_u)%v+p01d!39-l'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.Loader',
    'django.template.loaders.app_directories.Loader',
#     'django.template.loaders.eggs.Loader',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    # Uncomment the next line for simple clickjacking protection:
    # 'django.middleware.clickjacking.XFrameOptionsMiddleware',
)

ROOT_URLCONF = 'yum_repo_server.urls'

# Python dotted path to the WSGI application used by Django's runserver.
WSGI_APPLICATION = 'yum_repo_server.wsgi.application'

#TEMPLATE_DIRS = ('',)

INSTALLED_APPS = (
    # 'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'yum_repo_server.api',
    'yum_repo_server.daemon',
    # Uncomment the next line to enable the admin:
    # 'django.contrib.admin',
    # Uncomment the next line to enable admin documentation
    # 'django.contrib.admindocs',
)

INSTALL_DIR = os.environ.get('YUM_REPO_SERVER_INSTALL_DIR', '/opt/yum_repo_server')

TEMPLATE_DIRS = (
    os.path.join(INSTALL_DIR, 'templates'),
)

# Absolute path to the directory static files should be collected to.
# Don't put anything in this directory yourself; store your static files
# in apps' "static/" subdirectories and in STATICFILES_DIRS.
# Example: "/home/media/media.lawrence.com/static/"
STATIC_ROOT = os.path.join(INSTALL_DIR, 'static')

# load config for yum-repo-server
REPO_CONFIG = {'REPO_DIR'  : '/var/yum-repos', 
                'TEMP_DIR' : tempfile.gettempdir(),
                'DAEMON_USER' : 'root',
                'DAEMON_GROUP' : 'root',
                'REPO_CACHE_DIR' : tempfile.gettempdir(),
                'SCHEDULER_DAEMON_PIDFILE' : '/var/run/yum-repo-server/yum-repo-server.pid',
                'SCHEDULER_DAEMON_LOGGING_CONF': '/etc/yum-repo-server/schedulerDaemonLogging.conf',
                'SERVER_LOGGING_CONF': '/etc/yum-repo-server/serverLogging.conf',
               }

# get config file
if 'YUM_REPO_SERVER_CONFIG' in os.environ and len(os.environ['YUM_REPO_SERVER_CONFIG']) > 0:
    REPO_CONFIG_PATH = os.environ['YUM_REPO_SERVER_CONFIG']
else:
    REPO_CONFIG_PATH = '/etc/yum-repo-server/configuration.yaml'

# merge defaults with config from config file
if os.path.exists(REPO_CONFIG_PATH):
    config_file = open(REPO_CONFIG_PATH)
    new_config = yaml.load(config_file)
    config_file.close()
    REPO_CONFIG = dict(REPO_CONFIG.items() + new_config.items())

# determine the correct log.config files to use.
if not os.path.exists(REPO_CONFIG['SCHEDULER_DAEMON_LOGGING_CONF']):
    REPO_CONFIG['SCHEDULER_DAEMON_LOGGING_CONF']=os.path.join(INSTALL_DIR, 'defaults/schedulerDaemonLogging.conf')

if not os.path.exists(REPO_CONFIG['SERVER_LOGGING_CONF']):
    REPO_CONFIG['SERVER_LOGGING_CONF']=os.path.join(INSTALL_DIR, 'defaults/serverLogging.conf')

# set chunk size for temporay files
UploadedFile.DEFAULT_CHUNK_SIZE = 1024 * 1024

# set tmp directory for uploaded files
FILE_UPLOAD_TEMP_DIR = REPO_CONFIG['TEMP_DIR']
if not os.path.exists(FILE_UPLOAD_TEMP_DIR):
    os.makedirs(FILE_UPLOAD_TEMP_DIR)

# test suite
TEST_RUNNER = 'yum_repo_server.api.tests.TeamCityDjangoTestSuiteRunner'
