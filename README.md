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
* Have a look in <code>util/install_apache2_with_wsgi_ubuntu.sh</code>. This shell script installs all required dependencies for the yum repo server to run. The default setup is to run yum-repo-server behind an apache server.
  * If you are not running a Debian based system, have a look at the script anyway since it lists the dependencies you need to install.
* Run the tests : <code> python setup.py test</code>
* Try it out! Running <code>python manage.py runserver</code> will start up a django developement server, featuring the yum-repo-server.