IF( NOT CMAKE_BUILD_TYPE )

  IF( NOT ORCA_OS_WIN )
    # For gcc, RelWithDebInfo gives '-O2 -g'
    SET( CMAKE_BUILD_TYPE RelWithDebInfo )
  ELSE ( NOT ORCA_OS_WIN )
    # windows... a temp hack: VCC does not seem to respect the cmake
    # setting and always defaults to debug, we have to match it here.
    SET( CMAKE_BUILD_TYPE Debug )
  ENDIF( NOT ORCA_OS_WIN )

  MESSAGE( STATUS "Setting build type to '${CMAKE_BUILD_TYPE}'" )

ELSE ( NOT CMAKE_BUILD_TYPE )

  MESSAGE( STATUS "Build type set to '${CMAKE_BUILD_TYPE}' by user." )

ENDIF( NOT CMAKE_BUILD_TYPE )
