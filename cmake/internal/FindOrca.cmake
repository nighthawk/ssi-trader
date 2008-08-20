# Locate Orca home

# This module defines
# ORCA_HOME where to find include, lib, bin, etc.
# ORCA_FOUND, If set to 0, don't try to use Orca.

# start with 'not found'
SET( ORCA_FOUND 0 CACHE BOOL "Do we have Orca?" )

FIND_PATH( ORCA_HOME orca_manifest.cmake
    ${ORCA_HOME}
    $ENV{ORCA_HOME}
    # Test common installation points
    /usr/local
    /opt/orca
    /opt/orca-2.15.0+
    /opt/orca-2.15.0
    /opt/orca-2.14.0+
    /opt/orca-2.14.0
    /opt/orca-2.13.0+
    /opt/orca-2.13.0
    /opt/orca-2.12.0+
    /opt/orca-2.12.0
    /opt/orca-2.11.0+
    /opt/orca-2.11.0
    /opt/orca-2.10.0+
    /opt/orca-2.10.0
    /opt/orca-2.9.0+
    /opt/orca-2.9.0
    /opt/orca-2.8.0
    /opt/orca-2.7.0
    /opt/orca-2.6.0
    /opt/orca-2.5.0
    /opt/orca-2.4.0
    /opt/orca-2.3.0
    /opt/orca-2.2.0
    /opt/orca-2.2.0
    /opt/orca-2.1.0
    /opt/orca-2.0.0
    "C:/Program Files/orca/Include"
    C:/orca 
)

MESSAGE( STATUS "DEBUG: orca_manifest.cmake is apparently found in : ${ORCA_HOME}" )

# NOTE: if ORCA_HOME_INCLUDE_ORCA is set to *-NOTFOUND it will evaluate to FALSE
IF( ORCA_HOME )
    SET( ORCA_HOME ${ORCA_HOME} CACHE PATH "Orca installed directory" FORCE )
    SET( ORCA_FOUND 1 CACHE BOOL "Do we have Orca?" FORCE )
ENDIF( ORCA_HOME )
