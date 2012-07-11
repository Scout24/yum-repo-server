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


## Aim
The aim of this project is to provide a simple, straightforward and extensible implementation of a server that is able to manage YUM repositories. While creating a standalone YUM repository is easy, there is no easy way to manage many and at the time of writing no software fulfilling this purpose is available.

## Benefits
The yum-repo-server enables you to access repository management operations from other routines or automations, such as build servers or delivery chains.
For instance you can dynamically create a repository when needed (e.G. after compiling sources), upload RPMs into it, generate metadata and then use it right away!
Since consumers cannot differentiate between virtual and regular repositories, it is possible to change the repositories used by hosts dynamically in one simple operation (instead of fiddling on the file system level).