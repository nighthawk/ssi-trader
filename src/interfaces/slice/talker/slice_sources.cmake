#
# When you add a new .ice file, insert its name into this list. This is
# the only file which has to be updated.
#

#
# Since CMake doesn't know how to automatically track dependencies for .ice files, 
# these have to be entered manually in proper order: such that the depended-on
# files are listed first.
#
SET( ORCA_SLICE_SOURCE_FILES
#interfaces
    hello.ice
    someinterface.ice
)
