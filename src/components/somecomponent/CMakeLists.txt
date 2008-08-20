OPTION( build_SOME_COMPONENT "Try to build Some Component" ON )

IF( build_SOME_COMPONENT )

    # Use compilation settings commong to most components and utilities
    INCLUDE( ${ORCA_CMAKE_DIR}/UseComponentRules.cmake )
    
    # This gathers all the source files into the variable 'srcs'
    FILE( GLOB srcs *.cpp )
    
    GBX_ADD_EXECUTABLE( somecomponent ${srcs} )
    ORCA_GENERATE_CONFIG_FILE( somecomponent.def )

ENDIF( build_SOME_COMPONENT )
