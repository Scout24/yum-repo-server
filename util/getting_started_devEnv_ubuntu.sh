#!/bin/bash

## This install all libs necessary for developing the yum-repo-server
## The apt parts will only work on ubuntu based systems, but you can still use this script as a guide since it enumerates the necessary python libraries


OS=$(uname -v | grep -i 'ubuntu') && [[ "$OS" == "" ]] && echo "You are not Ubuntu! Too bad. I go away!" && exit 1

## Begin install steps
echo "Install pip"
sudo apt-get install python-pip

echo "Install Python Django framework"
sudo pip install django==1.3

echo "Install Piston extension for Django"
sudo pip install django-piston==0.2.3

echo "Install PyYaml for Python"
sudo pip install PyYAML

echo "Install Nose unittest extension for Python"
sudo apt-get install python-nose

echo "Install Teamcity Messages for Python with pip"
sudo pip install teamcity-messages

echo "Install createrepo"
sudo apt-get install createrepo

echo "Install python-rpm"
sudo apt-get install python-rpm

echo "Install pycurl"
sudo pip install pycurl
	
echo "Install lxml"
sudo apt-get install python-lxml
	
echo "Install python-daemon"
sudo pip install python-daemon

echo "Install apscheduler"
sudo pip install apscheduler

echo "Install lockfile"
sudo pip install lockfile

echo "Done!"

exit 0
