PYTHON_VERSION=$(/usr/bin/env python -c 'import sys; print(sys.version[:3])')
PYTHON_PACKAGE_DIR=$(/usr/bin/env python -c "from distutils.sysconfig import get_python_lib; print(get_python_lib())")

REPOCLIENT_PACKAGE_DIR="$PYTHON_PACKAGE_DIR/yum_repo_client"

if ! [ -d REPOCLIENT_PACKAGE_DIR ]; then
	
	ORIG_PACKAGE_DIR=$(echo $PYTHON_PACKAGE_DIR | sed -e "s/$PYTHON_VERSION/2.6/g")
	
	echo "Linking $ORIG_PACKAGE_DIR/yum_repo_client* to $PYTHON_PACKAGE_DIR"
	ln -s $ORIG_PACKAGE_DIR/yum_repo_client* $PYTHON_PACKAGE_DIR
	
	exit 0
fi