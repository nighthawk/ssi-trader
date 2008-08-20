
# If we're using gcc, make sure the version is OK.
IF( ${CMAKE_C_COMPILER} MATCHES gcc )

    EXEC_PROGRAM ( ${CMAKE_C_COMPILER} ARGS --version OUTPUT_VARIABLE GCC_VERSION )
    MESSAGE( STATUS "gcc version: ${GCC_VERSION}")

    # Why doesn't this work?
    #STRING( REGEX MATCHALL "gcc\.*" VERSION_STRING ${CMAKE_C_COMPILER} )

    IF( GCC_VERSION MATCHES ".*4\\.[0-9]\\.[0-9]" )
        SET( GCC_VERSION_OK 1 )
    ENDIF( GCC_VERSION MATCHES ".*4\\.[0-9]\\.[0-9]")

    ASSERT ( GCC_VERSION_OK
      "Checking gcc version - failed. Orca2 requires gcc v. 4.x"
      "Checking gcc version - ok"
      1 )
    
    IF( GCC_VERSION MATCHES ".*4\\.0.*" )
      # gcc 4.0.x
    ENDIF( GCC_VERSION MATCHES ".*4\\.0.*" )
    IF( GCC_VERSION MATCHES ".*4\\.1.*" )
      # gcc 4.1.x
      # gcc-4.1 adds stack protection, which makes code robust to buffer-overrun attacks
      #      (see: http://www.trl.ibm.com/projects/security/ssp/)
      # However for some reason this can result in the symbol '__stack_chk_fail_local' not being found.
      # So turn it off.
      # Tobi: it looks like stack protection is off by default from version gcc 4.1.2, so we don't need this any more.
      # Will keep it for now, it doesn't hurt.
      ADD_DEFINITIONS( -fno-stack-protector )
    ENDIF( GCC_VERSION MATCHES ".*4\\.1.*" )


ENDIF( ${CMAKE_C_COMPILER} MATCHES gcc )
