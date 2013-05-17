# yum-repo-server 
=================
The yum-repo-server is a server that allows you to host and manage YUM repositories using a RESTful API.

## Main features
* RESTful api for repository management (including creation, metadata generation, RPM upload, RPM propagation,...)
* Configurable scheduling system for periodic metadata generation on repositories with high activity
* Repository cleanup routines
* Graphical web interface to browse repositories and their contents
* Link system to create virtual repositories that can dynamically point to other repositories
* Easily extensible due to good test coverage
* Propagation of RPMs from one staging repository to the next
* Command line wrapper for more comfort, see below

## Aim
The aim of this project is to provide a simple, straightforward and extensible implementation of a server that is able to manage YUM repositories. 
While creating a standalone YUM repository is easy, there was no easy way to manage many such repositories at the time of writing.

## Intent
Our company is migrating towards a CLD-friendly deployment solution. Our solution involves release repositories that need to be dynamically referenceable in order to update hosts or entire host groups without changing the host's repositories. This is done like so :

![Image of intended usage of the yum-repo-server](https://github.com/ImmobilienScout24/yum-repo-server/raw/master/docs/yrs_usecase.jpg)

## Benefits
* The yum-repo-server enables you to access repository management operations from other routines or automations, such as build servers or delivery chains.
  * For instance you can dynamically create a repository when needed (e.G. after compiling sources), upload RPMs into it, generate metadata and then use it right away!
* The virtual repository system provides an additional layer of abstraction over repositories and allows you to create "fake" (virtual) repositories that forward any requests they obtain to a real repository.
  * Since consumers cannot differentiate between virtual and regular repositories, it is possible to change the repositories used by hosts dynamically in one simple operation (instead of fiddling on the file system level in <code>/etc/yum/repos.d/</code> for instance).
    * As a consequence, the virtual repository system enables you to use one (virtual) repository for a group of hosts, and change the link as needed, e.G. when updating packages.
* The yum-repo-server comes with built-in cleanup and metadata generation routines, meaning you do not need to use other tools (like CRON jobs) to manage repositories  

## License  
The yum-repo-server is licensed under the [GPLv3](http://www.gnu.org/licenses/quick-guide-gplv3.html)

## Getting started
* Install the packages that are not part of PyPi :
```bash
sudo apt-get install python-rpm
python-lxml
python2.7-dev  
python-pycurl  
PyYAML
```

* Install the pip packages in a virtualenv (remember to use the system-site-packages switch to make the modules that are not on PyPi available to the virtualenv)
```bash
virtualenv --system-site-packages ve  
. ve/bin/activate  
pip install django==1.3 django-piston nose python-daemon lockfile stdeb mockito argcomplete
```

* Build it : <code>python setup.py build</code>

* Run the tests : <code> python setup.py test</code>

* Install it : <code>python setup.py install</code>

* Try it out! Running <code>python manage.py runserver</code> will start up a django developement server (not for production!!) featuring the yum-repo-server.
	* This development server is fully fledged and you can use it to determine if the yum-repo-server is what you want very quickly.
	* Use the included yum-repo-client for more comfortable tryouts. (cd into client/ and install it with <code>python setup.py install</code>)

## Production usage
For production usage we recommend an *apache webserver (httpd)* with *mod_wsgi*. If you build a RPM by <code>python setup.py bdist_rpm</code> an apache configuration file is automatically included. But the <code>wsgi.py</code> should work as well with other WSGI-compatible servers like CherryPy, twisted.web, Gunicorn, etc.

## How it works
### Technology
The yum-repo-server relies on django and piston to handle browser and API requests made through HTTP. You need to put a HTTP web server in front of the yum-repo-server, apache with mod_wsgi will do fine. Most of the scheduling routines use the APScheduler (Advanced Python Scheduler). In order to generate repository metadata, the createrepo package is used.
Have a look at updaterepo [https://github.com/heroldus/updaterepo] if you need a more performant createrepo.

### Repository location
At the core of the yum-repo-server is a directory that contains repository information and contents.
This directory's location is stored in a settings.py file, like so :
<code>
REPO_CONFIG = {'REPO_DIR'  : '/var/yum-repos', [shortened]  }
</code>
This makes it easy to backup or replicate your repository.

### Repository usage
In a nutshell, when yum checks for updates it sends HTTP GET requests to repositories it is aware of (usually through repository files in <code> /etc/yum/repos.d/</code>) and queries repository metadata.
If it decides a package has to be updated (or installed) it will then directly download the RPM package through a HTTP request.
This is handled by django and quite straightforward.

### Virtual repositories
A virtual repository does look exactly like a regular repository for consumers, but it is actually an empty repository that contains a YAML file named <code>repo.yaml</code>. The file contains an entry with a relative path to a regular repository, and requests to the virtual repository are rerouted to the regular one.

### Periodic metadata generation
The metadata generation is located in a YAML file called `metadata-generation.yaml` that lives in the repository it describes.
The file looks like this :

```yaml
generation_type : scheduled
generation_interval : 40
rpm_max_keep : 3
```

This will schedule a periodic createrepo that will be executed every 40 seconds.
`rpm_max_keep` means there will also be a cleanup routine before the createrepo that will delete older
RPMs when there are more than three RPMs with the same canonical name.
You may omit `rpm_max_keep` to disable the cleanup routine and set `generation_type` to `manual` or remove the file
if you do not wish to have a periodic createrepo scheduled.

### API requests
API requests are handled by piston and use a REST like format.
For maximal comfort, use the yum-repo-client.
The examples below should give you a good understanding of how the requests look like.

#### Repository creation
Creating a new repository involves sending a POST request with the name of the repository in the body to <code>$host/$repo_base</code>. 
This will create a new resource (the new repository) underneath the repository base, which means you can access your new repository at <code>$host/$repo_base/$new_repo_name</code>

#### Repository deletion
A static repository can be deleted when sending a DELETE request to the repository (/repo/repo-to-delete). It can be protected from deletion when its name is listed within the <code>/etc/yum-repo-server/non-deletable-repositories</code> file. 
Virtual repositories that were linked to the deleted static repository, will not be deleted or changed. The virtual repositories will deliver HTTP 404 sites as long as the static repository does not exist again or the link is changed manually.

#### Upload to an existing repository
As a consequence, uploading a RPM to an existing repository involves sending a POST request containing the RPM file in a form element called rpmFile. The request is send to <code>$host/$repo_base/$repo_name</code>
It creates a new resource underneath <code>$repo_name</code>. 
The RPM can then be retrieved with a GET request sent to
<code>$host/$repo_base/$repo_name/$rpm_architecture/$rpm_filename</code>.

#### Generating repository metadata
Generating metadata involves a POST request to <code>$host/$repo_base/$repo_name/repodata</code> since it creates a new resource (the actual metadata files) underneath <code>repodata/</code>.

#### Propagate a RPM from one repository to another
You can propagate a RPM from a source repository to a destination repository on the same host by sending a POST request to <code>$host/propagation/</code> with parameter <code>source</code> and <code>destination</code>.
<code>source</code> must be <code>$source-repo-name/$architecture/artifact-name.rpm</code>. <code>destination</code> is just name of the target repository.
Propagation does not work with virtual repositories.
For example:
<code>
curl -F "source=test-repo/noarch/ test-artifact&destination=test-repo2" http://myyum-repo-server/propagation/
</code>
will search for the latest <code>test-artifact-XX-X.noarch.rpm</code> and propagate the rpm from <code>test-repo</code> repository to <code>test-repo2</code>.

#### List static or virtual repositories
You can retrieve a list of static or virtual repositories for static repos via
<code>
http://myyum-repo-server/repo.txt
</code>
for virtual repos:
<code>
http://myyum-repo-server/repo/virtual.txt
</code>
Optionally you can get the destination for virtual repositories with the showDestination parameter. If set to true the list will contain entries with the following pattern: <code>repo_name:destination</code>. The destination is the path to the static repository or it could also be a url to an external repository.

To filter the list you have several url parameters:
 * Filter by name regex: <code>http://myyum-repo-server/repo.txt?name=any_regex.*</code>
 * Filter by tags: <code>http://myyum-repo-server/repo.txt?tag=tag1,tag2</code> This will retrieve all repositories marked with tag1 or tag2.
 * Filter by tags exclusive: <code>http://myyum-repo-server/repo.txt?notag=tag1,tag2</code> This will retrieve all repositories marked not marked with tag1 or tag2.
 * Filter by newer then X days: <code>http://myyum-repo-server/repo.txt?newer=10</code> This will retrieve all repositories newer then 10 days.
 * Filter by older then X days: <code>http://myyum-repo-server/repo.txt?older=10</code> This will retrieve all repositories older then 10 days.

All filters are concatable and are combined via _and_, so <code>http://myyum-repo-server/repo.txt?older=10&newer=30</code> will retrieve all repositories older then 10 days and newer then 30 days.
