#
# Regular cron jobs for the uecide package
#
0 4	* * *	root	[ -x /usr/bin/uecide_maintenance ] && /usr/bin/uecide_maintenance
