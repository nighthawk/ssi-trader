##### Compiling
- convert them to ruby using slice2java command
		slice2java -I<path to find included files> <files.ice>
	example:
		cd slice
		slice2java -I/home/adrian/Desktop/orca/orca/src/interfaces/slice/ pathevaluator.ice

- set environment variables
		export ICEJ_PATH=/usr/share/java
		export CLASSPATH=$CLASSPATH:.:../lib/jade/jade.jar:$ICEJ_PATH/Ice.jar:../classes

- compile
		javac -d ../classes GenericRobot.java slice/orca/*.java slice/talker/*.java


##### Running
- set environment variables
		export CLASSPATH=$CLASSPATH:.:../lib/jade/jade.jar:$ICEJ_PATH/Ice.jar:../classes

- run with proper options
		java jade.Boot "<nickname>:<ClassName>(<param1> <param2> <...>)"
	example:
		java jade.Boot "test:GenericRobot(12000 1.3)"
