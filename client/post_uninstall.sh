# in final deinstallation remove symbolic links if exists
if [ "$1" == "0" ]; then

	PYTHON_VERSION=$(/usr/bin/env python -c 'import sys; print(sys.version[:3])')
	PYTHON_PACKAGE_DIR=$(/usr/bin/env python -c "from distutils.sysconfig import get_python_lib; print(get_python_lib())")
	
	REPOCLIENT_PACKAGE_DIR="$PYTHON_PACKAGE_DIR/yum_repo_client"
	
	if [ -h $REPOCLIENT_PACKAGE_DIR ]; then
		rm $PYTHON_PACKAGE_DIR/yum_repo_client*
		
		exit 0
	fi 
fi