/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_TIME_ICE
#define ORCA2_TIME_ICE

module orca
{

/*!
    @brief Unix absolute time
*/
struct Time
{
    //! Number of seconds
    int seconds;
    //! Number of microseconds
    int useconds;
};

/*!
    Time of day.
*/
struct TimeOfDay
{
    //! Hour [0..23]
    int hours;
    //! Minutes [0..59]
    int minutes;
    //! Seconds [0.0..59.9999(9)]
    double seconds;
};

/*!
    @brief Date of the year.
*/
struct Date
{    
    //! Day [1..31]
    int day;
    //! Month [1..12]
    int month;
    //! Year (2006 is 2006)
    int year;
};

}; // module

#endif
