/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#ifndef STATIC_DRIVER_H
#define STATIC_DRIVER_H

#include "driver.h"

namespace simlocaliser
{

class StaticDriver : public Driver
{
public:

    StaticDriver( const orca::Frame2d & pose );
    virtual ~StaticDriver() {}; 

    // returns: 0 = success, non-zero = failure
    virtual int enable() { return 0; };
    virtual int repair() { return 0; };
    virtual int disable() { return 0; };

    virtual int read( orca::Localise2dData& location2d );

private:

    orca::Localise2dData location2d_;
};

} // namespace

#endif
