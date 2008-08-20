#
# Figure out if we are in the main Orca project or not
# If it is, all necessary files are in this project.
# Otherwise, we need to find Orca installation directory.
#
# This file should be copied verbatim into [PROJECT]/cmake/local
# directory of satelite projects.
#
INCLUDE( cmake/internal/Assert.cmake )

IF( ${PROJECT_NAME} STREQUAL "orca" )
    MESSAGE( STATUS "Setting project type to ORCA MOTHERSHIP" )
    SET( ORCA_MOTHERSHIP 1 )

    SET( ORCA_CMAKE_DIR ${${PROJECT_NAME}_SOURCE_DIR}/cmake )

ELSE ( ${PROJECT_NAME} STREQUAL "orca" )
    MESSAGE( STATUS "Setting project type to ORCA SATELLITE" )
    SET( ORCA_MOTHERSHIP 0 )

    # If this is NOT the Orca project, we need to find Orca installation
    IF( DEFINED ORCA_HOME AND ORCA_HOME )
        # Orca home is specified with a command line option or it's already in cache
        MESSAGE( STATUS "Orca location was specified or using cached value: ${ORCA_HOME}" )
    ELSE ( DEFINED ORCA_HOME AND ORCA_HOME  )
        # Find Orca Installation
        # Will search several standard places starting with an env. variable ORCA_HOME
        INCLUDE( cmake/internal/FindOrca.cmake )

        ASSERT( ORCA_FOUND 
            "Looking for Orca - not found. Please install Orca, ** delete CMakeCache.txt **, then re-run CMake." 
            "Looking for Orca - found in ${ORCA_HOME}" 
            1 )
    ENDIF( DEFINED ORCA_HOME AND ORCA_HOME  )

    #
    # Load Orca manifest
    #
    INCLUDE( ${ORCA_HOME}/orca_manifest.cmake )
    MESSAGE( STATUS "Loaded Orca manifest")

    SET( ORCA_BIN_DIR ${ORCA_HOME}/bin )
    SET( ORCA_LIB_DIR ${ORCA_HOME}/lib/orca )
    SET( ORCA_INCLUDE_DIR ${ORCA_HOME}/include/orca )
    SET( ORCA_SHARE_DIR ${ORCA_HOME}/share/orca )
    SET( ORCA_CMAKE_DIR ${ORCA_HOME}/share/orca/cmake )
    SET( ORCA_SLICE_DIR ${ORCA_HOME}/share/orca/slice )

ENDIF( ${PROJECT_NAME} STREQUAL "orca" )

MESSAGE( STATUS "Using custom CMake scripts in ${ORCA_CMAKE_DIR}" )

#
# The rest is done by a script common to both Orca and derived projects
#
INCLUDE( ${ORCA_CMAKE_DIR}/Setup.cmake )
