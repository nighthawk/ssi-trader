# Install script for directory: /home/adrian/Desktop/orca/orca/src/components/simlocaliser

# Set the install prefix
IF(NOT DEFINED CMAKE_INSTALL_PREFIX)
  SET(CMAKE_INSTALL_PREFIX "/usr/local")
ENDIF(NOT DEFINED CMAKE_INSTALL_PREFIX)
STRING(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
IF(NOT CMAKE_INSTALL_CONFIG_NAME)
  IF(BUILD_TYPE)
    STRING(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  ELSE(BUILD_TYPE)
    SET(CMAKE_INSTALL_CONFIG_NAME "RelWithDebInfo")
  ENDIF(BUILD_TYPE)
  MESSAGE(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
ENDIF(NOT CMAKE_INSTALL_CONFIG_NAME)

# Set the component getting installed.
IF(NOT CMAKE_INSTALL_COMPONENT)
  IF(COMPONENT)
    MESSAGE(STATUS "Install component: \"${COMPONENT}\"")
    SET(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  ELSE(COMPONENT)
    SET(CMAKE_INSTALL_COMPONENT)
  ENDIF(COMPONENT)
ENDIF(NOT CMAKE_INSTALL_COMPONENT)

# Install shared libraries without execute permission?
IF(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  SET(CMAKE_INSTALL_SO_NO_EXE "1")
ENDIF(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)

FILE(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/bin" TYPE EXECUTABLE FILES "/home/adrian/Desktop/orca/orca/src/components/simlocaliser/CMakeFiles/CMakeRelink.dir/simlocaliser")
FILE(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/orca/def" TYPE FILE FILES "/home/adrian/Desktop/orca/orca/src/components/simlocaliser/simlocaliser.def")
FILE(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/orca/cfg" TYPE FILE FILES "/home/adrian/Desktop/orca/orca/src/components/simlocaliser/./simlocaliser.cfg")
FILE(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/orca/xml" TYPE FILE FILES "/home/adrian/Desktop/orca/orca/src/components/simlocaliser/./simlocaliser.xml")
IF(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  INCLUDE("/home/adrian/Desktop/orca/orca/src/components/simlocaliser/stage/cmake_install.cmake")

ENDIF(NOT CMAKE_INSTALL_LOCAL_ONLY)
