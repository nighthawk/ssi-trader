#
# This file doesn't actually build the ice files.  
# That's done in src/interfaces/<language>/orca/CMakeLists.txt,
# using the list of .ice files given in slice_sources.cmake.
#


#
# Install all the .ice files, so other projects can use them.
# Install sub-directory will be the same as the current sub-directory
# which is typically the same as the name of the namespace, e.g. 'orca'
#
GET_FILENAME_COMPONENT( INTERFACE_NAMESPACE ${CMAKE_CURRENT_SOURCE_DIR} NAME )

INSTALL_FILES( /slice/${INTERFACE_NAMESPACE} .*\\.ice$ )
MESSAGE( STATUS "Will install Slice files into ${PROJECT_INSTALL_DIR}/slice/${INTERFACE_NAMESPACE}" )
