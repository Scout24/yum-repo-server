chkconfig --add yum_repo_daemon
if [ $1 -eq 2 ]; then
  service yum_repo_daemon status
  if [ $? -eq 0 ]; then
      service yum_repo_daemon restart
  fi
fi