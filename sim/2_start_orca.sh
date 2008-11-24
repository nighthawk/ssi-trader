#!/bin/bash

BASE_DIR=/opt/mult

# start ice registry
#cd $BASE_DIR/icereg
#icegridregistry --Ice.Config=icegridregistry.cfg &

# start icestorm
#cd $BASE_DIR/icestorm
#icebox --Ice.Config=icebox_icestorm.cfg &

# start components
cd $BASE_DIR/talker/sim
ogmaploader > ogmaploader.log &
pathplanner > pathplanner.log &

cd $BASE_DIR/talker/build/src/components/pathevaluator
./pathevaluator 