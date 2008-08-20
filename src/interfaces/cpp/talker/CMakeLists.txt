# The guts of this CMakeLists.txt file are reused to build interfaces of satellite
# projects. For this reason it is stored in a central location and then installed.
#    Orca project      : see [ORCA-SOURCE]/cmake
#    Satellite project : see [ORCA-INSTALL]/cmake

INCLUDE( ${ORCA_CMAKE_DIR}/BuildLibProjectInterfacesCpp.cmake )
