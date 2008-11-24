#!/bin/bash

BASE_DIR=/opt/mult

# connect to deep thought
# sshfs $USER@deep:Projects/Multi-robots/code /opt/mult

# start ice registry
cd $BASE_DIR/icereg
icegridregistry --Ice.Config=icegridregistry.cfg > /dev/null &

# start icestorm
cd $BASE_DIR/icestorm
icebox --Ice.Config=icebox_icestorm.cfg > /dev/null &