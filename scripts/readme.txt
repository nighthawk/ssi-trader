##### How to use ICE interfaces

- convert them to ruby using slice2rb command
		slice2rd -I<path to find included files> <files.ice>
	example:
		slice2rb -I/home/adrian/Desktop/orca/orca/src/interfaces/slice/ goalevaluator.ice
- then move the created *.rb files to their respective folder, usually 
  depending on their module name