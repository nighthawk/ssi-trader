/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2007 Alexei Makarenko
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#include <iostream>
#include <orcaice/orcaice.h>

#include "mainthread.h"

using namespace std;
using namespace helloclient;

MainThread::MainThread( const orcaice::Context& context ) :
    SubsystemThread( context.tracer(), context.status(), "MainThread" ),
    context_(context)
{
    subStatus().setMaxHeartbeatInterval( 20.0 );
}

void
MainThread::walk()
{
    // multi-try function
    orcaice::activate( context_, this, subsysName() );

    //
    // Read configuration settings
    //
    std::string prefix = context_.tag() + ".Config.";

    int sleepIntervalMs = orcaice::getPropertyAsIntWithDefault( context_.properties(),
            prefix+"SleepIntervalMs", 1000 );

    subStatus().ok( "Initialized" );

    //
    // Main loop
    //   
    subStatus().setMaxHeartbeatInterval( sleepIntervalMs * 3.0/1000.0 );

    while( !isStopping() )
    {
        context_.tracer().debug( "Running main loop", 5 );
        subStatus().heartbeat();

        // here we can do something useful

        IceUtil::ThreadControl::sleep(IceUtil::Time::milliSeconds(sleepIntervalMs));
    }
}
