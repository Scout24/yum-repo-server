if [ $1 -eq 0 ]; then
  service yum_repo_daemon stop
  chkconfig --del yum_repo_daemon
fi