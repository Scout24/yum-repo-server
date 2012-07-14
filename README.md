# yum-repo-server
===============
The yum-repo-server is a server that allows you to host and manage YUM repositories using a RESTful API.

## Main features
* RESTful api for repository management (including creation, metadata generation, RPM upload, ...)
* Configurable scheduling system for periodic metadata generation on repositories with high activity
* Repository cleanup routines
* Graphical web interface to browse repositories and their contents
* Link system to create virtual repositories that can dynamically point to other repositories
* Easily extensible due to good test coverage
* Command line wrapper for more comfort, see [http://github.com/is24-herold/yum-repo-client]


## Aim
The aim of this project is to provide a simple, straightforward and extensible implementation of a server that is able to manage YUM repositories. 
While creating a standalone YUM repository is easy, there was no easy way to manage many such repositories at the time of writing.

## Benefits
* The yum-repo-server enables you to access repository management operations from other routines or automations, such as build servers or delivery chains.
  * For instance you can dynamically create a repository when needed (e.G. after compiling sources), upload RPMs into it, generate metadata and then use it right away!
* The virtual repository system provides an additional layer of abstraction over repositories and allows you to create "fake" (virtual) repositories that forward any requests they obtain to a real repository.
  * Since consumers cannot differentiate between virtual and regular repositories, it is possible to change the repositories used by hosts dynamically in one simple operation (instead of fiddling on the file system level in <code>/etc/yum/repos.d/</code> for instance).
    * As a consequence, the virtual repository system enables you to use one (virtual) repository for a group of hosts, and change the link as needed, e.G. when updating packages.
* The yum-repo-server comes with built-in cleanup and metadata generation routines, meaning you do not need to use other tools (like CRON jobs) to manage repositories


## Getting started
* Build it : <code>python setup.py build</code>
* Install it : <code>python setup.py install</code>
* Run the tests : <code> python setup.py test</code>
* Try it out! Running <code>python manage.py runserver</code> will start up a django developement server, featuring the yum-repo-server.

## How it works
### Technology
The yum-repo-server relies on django and piston to handle browser and API requests made through HTTP. You need to put a HTTP web server in front of the yum-repo-server, apache with mod_wsgi will do fine. Most of the scheduling routines use the APScheduler (Advanced Python Scheduler). In order to generate repository metadata, the createrepo package is used.
Have a look at updaterepo [https://github.com/is24-herold/updaterepo] if you need a more performant createrepo.

### Repository location
At the core of the yum-repo-server is a directory that contains repository information and contents.
This directory's location is stored in a setup.py file, like so :
<code>
REPO_CONFIG = {'REPO_DIR'  : '/var/yum-repos', [shortened]  }
</code>
This makes it easy to backup or replicate your repository.

### Repository usage
In a nutshell, when yum checks for updates it sends HTTP GET requests to repositories it is aware of (usually through repository files in <code> /etc/yum/repos.d/</code> and queries repository metadata.
If it decides a package has to be updated (or installed) it will then directly download the RPM package through a HTTP request.
This is handled by django and quite straightforward.

### Virtual repositories
A virtual repository does look exactly like a regular repository for consumers, but it is actually an empty repository that contains a YAML file named <code>repo.yaml</code>. The file contains an entry with a relative path to a regular repository, and requests to the virtual repository are rerouted to the regular one.

### Periodic metadata generation
The metadata generation is located in a YAML file that lives in the repository it describes.
The file looks like this :
<code>
generation_type : scheduled
generation_interval : 40
rpm_max_keep : 3
</code>
This will schedule a periodic createrepo that will be executed every 40 seconds.
rpm_max_keep means there will also be a cleanup routine before the createrepo that will delete older
RPMs when there are more than three RPMs with the same canonical name.
You may omit rpm_max_keep to disable the cleanup routine and set generation_type to 'manual' or remove the file
if you do not wish to have a periodic createrepo scheduled.

### API requests
API requests are handled by piston and use a REST like format.
For maximal comfort, use the yum-repo-client, see [http://github.com/is24-herold/yum-repo-client].
The examples below should give you a good understanding of how the requests look like.

#### Repository creation
Creating a new repository involves sending a POST request with the name of the repository in the body to <code>$host/$repo_base</code>. 
This will create a new resource (the new repository) underneath the repository base, which means you can access your new repository at <code>$host/$repo_base/$new_repo_name</code>

#### Upload to an existing repository
As a consequence, uploading a RPM to an existing repository involves sending a POST request containing the RPM file in a form element called rpmFile. The request is send to <code>$host/$repo_base/$repo_name</code>
It creates a new resource underneath <code>$repo_name</code>. 
The RPM can then be retrieved with a GET request sent to
<code>$host/$repo_base/$repo_name/$rpm_architecture/$rpm_filename</code>.

#### Generating repository metadata
Generating metadata involves a POST request to <code>$host/$repo_base/$repo_name/repodata</code> since it creates a new resource (the actual metadata files) underneath <code>repodata/</code>.