/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2007 Alexei Makarenko
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */
 
#ifndef MAIN_THREAD_H
#define MAIN_THREAD_H

#include <orcaice/subsystemthread.h>
#include <orcaice/context.h>

namespace helloserver
{

class MainThread: public orcaice::SubsystemThread
{    	
public:
    MainThread( const orcaice::Context& context );

    // from SubsystemThread
    virtual void walk();

private:
    orcaice::Context context_;
};

} // namespace

#endif
