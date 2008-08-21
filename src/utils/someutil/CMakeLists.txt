OPTION( build_SOME_UTILITY "Try to build Some Utility" OFF )

IF( build_SOME_UTILITY )

    # Use compilation settings commong to most components and utilities
    INCLUDE( ${ORCA_CMAKE_DIR}/UseComponentRules.cmake )
    
    # This gathers all the source files into the variable 'srcs'
    FILE( GLOB srcs *.cpp )
    
    GBX_ADD_EXECUTABLE( someutil ${srcs} )

ENDIF( build_SOME_UTILITY )
