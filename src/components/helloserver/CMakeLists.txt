ORCA_SET_COMPONENT_NAME( HelloServer )

SET( build TRUE )
# Check for human input
GBX_REQUIRE_OPTION( build EXE ${APP_NAME} OFF )

# libs on which our code depends would go here
# SET( dep_libs ??? )
# GBX_REQUIRE_TARGETS( build EXE ${APP_NAME} ${dep_libs} )

IF( build)

    INCLUDE( ${ORCA_CMAKE_DIR}/UseComponentRules.cmake )

    FILE( GLOB srcs *.cpp )
    
    # build stand-alone component  
    ORCA_WRAP_MAINTHREAD_INTO_APP( ${APP_NAME} ${APP_CTRLC_HANDLER} ${COMP_NAMESPACE} ${COMP_NAME} ${srcs} )
#     TARGET_LINK_LIBRARIES( ${APP_NAME} ${dep_libs} )
    
    # build IceBox service
    IF( ORCA_BUILD_SERVICES )
        ORCA_WRAP_MAINTHREAD_INTO_SERVICE( ${SERVICE_NAME} ${COMP_NAMESPACE} ${COMP_NAME} ${srcs} )
#         TARGET_LINK_LIBRARIES( ${SERVICE_NAME} ${dep_libs} )    
    ENDIF( ORCA_BUILD_SERVICES )

    ORCA_GENERATE_CONFIG_FILE( ${APP_NAME}.def )

ENDIF( build )
