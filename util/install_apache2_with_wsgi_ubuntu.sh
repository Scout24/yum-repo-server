#!/bin/bash

## This install all libs/services and configure the workspace for Python/Apache/WSGI/Django ONLY for Ubuntu

#set -xv

OS=$(uname -v | grep -i 'ubuntu') && [[ "$OS" == "" ]] && echo "You are not Ubuntu! Too bad. I go away!" && exit 1

## Getting my globals
PWD=$(pwd) 
LOCAL_PROJECT_DIR=$(echo $PWD | sed -e 's/\/util.*//g')
LOCAL_PROJECT_PYTHON_DIR="$LOCAL_PROJECT_DIR/src/main/python"
WSGI_BINDING_CONF=$(find $LOCAL_PROJECT_DIR -name is24-yum-repo-server_wsgi_bindung.conf)
! [[ -f $WSGI_BINDING_CONF ]] && echo "Apache WSGI conf file NOT found. Exit." && exit 1
WSGI_BINDING_CONF_DIR=$(echo $WSGI_BINDING_CONF | sed -e 's/\/is24-yum-repo-server_wsgi_bindung.conf//g')
WSGI_SCRIPT_ALIAS_HANDLER=$(grep 'WSGIScriptAlias' $WSGI_BINDING_CONF | cut -d' ' -f3)
WSGI_SCRIPT_TMP_DIR=$(grep 'SetEnv TMPDIR' $WSGI_BINDING_CONF | cut -d' ' -f3)
WSGI_SCRIPT_ALIAS_MAIN_PATH=$(echo $WSGI_SCRIPT_ALIAS_HANDLER | sed -e 's/\/yum_repo_server.*//g') 



echo -e "\n######################################"
echo -e "Local Project Path \t\t:\t $LOCAL_PROJECT_DIR"
echo -e "Local Project Python Path \t:\t $LOCAL_PROJECT_PYTHON_DIR"
echo -e "WSGI Apache2 Config \t\t:\t $WSGI_BINDING_CONF"
echo -e "WSGI Apache2 Config Path \t:\t $WSGI_BINDING_CONF_DIR"
echo -e "WSGI Script Handler \t\t:\t $WSGI_SCRIPT_ALIAS_HANDLER"
echo -e "WSGI Script Alias Path \t\t:\t $WSGI_SCRIPT_ALIAS_MAIN_PATH"
echo -e "WSGI Tmpdir  \t\t\t:\t $WSGI_SCRIPT_TMP_DIR"
echo -e "######################################\n"
while true; do
	read -p "Is it okay to proceed? (y/n) " yn
	        case $yn in
			        [Yy]* ) break;;
				        [Nn]* ) echo Aborting..; exit;;
					        * ) echo "Please answer with y, Y, n or N.";;
						    esac
					    done

## Begin install steps
echo "This script try to install the wsgi configuration for yum-repo-server on ubuntu"

echo "First we will install apache2" 
sudo apt-get install apache2

echo "Second we will install libapache2-mod-wsgi for apache2"
sudo apt-get install libapache2-mod-wsgi

echo "Install pip"
sudo apt-get install python-pip

echo "Install Python Django framework"
sudo pip install django==1.3

echo "Install Piston extension for Django"
sudo pip install django-piston==0.2.3

echo "Install Yaml for Python"
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

echo "Show if mod_wsgi is loaded already"
sudo /usr/sbin/apachectl -t -D DUMP_MODULES

echo "Check if /etc/apache2/apache2.conf exists"
! [[ -f /etc/apache2/apache2.conf ]] && echo "/etc/apache2/apache2.conf does not exist. Exit." && exit 1

checkInclude=$(grep -i "Include $WSGI_BINDING_CONF_DIR" /etc/apache2/apache2.conf)
[[ "$checkInclude" == "" ]] && echo "Add include $WSGI_BINDING_CONF_DIR in /etc/apache2/apache2.conf" && sudo chmod 777 /etc/apache2/apache2.conf && echo "Include $LOCAL_PROJECT_DIR/src/main/etc/httpd/conf/main/" >> /etc/apache2/apache2.conf && sudo chmod 644 /etc/apache2/apache2.conf

echo "Create symlink from $LOCAL_PROJECT_PYTHON_DIR to $WSGI_SCRIPT_ALIAS_MAIN_PATH"
! [[ -h $WSGI_SCRIPT_ALIAS_MAIN_PATH ]] && sudo ln -s $LOCAL_PROJECT_PYTHON_DIR $WSGI_SCRIPT_ALIAS_MAIN_PATH

echo "Make chmod 755 for all files in $WSGI_SCRIPT_ALIAS_MAIN_PATH/yum_repo_server"
sudo chmod 755 $WSGI_SCRIPT_ALIAS_MAIN_PATH/yum_repo_server/* 

echo "Restart Apache2 service"
sudo /etc/init.d/apache2 restart
