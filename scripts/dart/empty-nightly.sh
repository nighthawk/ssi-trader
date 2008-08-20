#!/bin/bash

dashboard=$HOME/ctests/orca/empty-nightly

# compile with gcc-4.2
logfile=$dashboard/empty.log
echo ---------------------------------------- >> $logfile
date >> $logfile
echo ---------------------------------------- >> $logfile
# build and test
/usr/bin/ctest -S $dashboard/empty-nightly-linux-gcc42.cmake -V >> $logfile 2>&1
