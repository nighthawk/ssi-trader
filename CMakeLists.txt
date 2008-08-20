#
# Test CMake version
#
CMAKE_MINIMUM_REQUIRED( VERSION 2.4 FATAL_ERROR )

#
# Enter some information about the project
# This is the only configurable part of the build system
#
# project name
PROJECT( talker )

# project version string.
SET( GBX_PROJECT_VERSION            0.0.1       CACHE STRING "Version of Orca satellite project" )
SET( PROJECT_INTERFACE_LIB					TalkerInterfaces )

#
# The rest is done by a script
#
INCLUDE( cmake/internal/bootstrap.cmake )
