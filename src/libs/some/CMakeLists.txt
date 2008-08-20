ORCA_SET_LIBRARY_NAME( TalkerSome )

SET( build TRUE )
GBX_REQUIRE_OPTION( build LIB ${LIB_NAME} ON )

IF( build )

    INCLUDE( ${ORCA_CMAKE_DIR}/UseComponentRules.cmake )
    
    FILE( GLOB srcs *.cpp )
#     SET( dep_libs ${ext_libs} ${int_libs} ${other_libs} )
    
    GBX_ADD_LIBRARY( ${LIB_NAME} SHARED ${srcs} )
    
#     TARGET_LINK_LIBRARIES( ${LIB_NAME} ${dep_libs} )
    
#     IF( ORCA_BUILD_TESTS )
#         ADD_SUBDIRECTORY( test )
#     ENDIF( ORCA_BUILD_TESTS )

ENDIF( build )
