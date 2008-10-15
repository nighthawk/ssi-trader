##### prepare
- convert them to ruby using slice2java command
		slice2java -I<path to find included files> <files.ice>
	example:
		cd slice
		slice2java -I/home/adrian/Desktop/orca/orca/src/interfaces/slice/ pathevaluator.ice

- set environment variables
		export ICEJ_PATH=../lib/ice
		export CLASSPATH=$CLASSPATH:.:../lib/jade/jade.jar:../lib/ice/Ice.jar:../classes
		


##### compile
		javac -d ../classes GenericRobot.java slice/orca/*.java slice/talker/*.java


##### running

- run with proper options
		java jade.Boot "<nickname>:<ClassName>(<param1> <param2> <...>)"
	example:
		java jade.Boot "red:GenericRobot(red info)"
		
		java jade.Boot -gui "red:GenericRobot(red spam) blue:GenericRobot(blue info) green:GenericRobot(green info) yellow:GenericRobot(yellow info) black:GenericRobot(black info)"
