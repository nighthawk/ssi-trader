/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */
 
#ifndef MAINTHREAD_H
#define MAINTHREAD_H

#include <orcaice/subsystemthread.h>
#include <orcaice/context.h>
#include <hydronavutil/pose.h>
#include <orca/pathplanner2d.h>
#include <memory>
#include <orcaifaceimpl/storingconsumers.h>

namespace pathevaluator
{

class MainThread : public orcaice::SubsystemThread
{

public: 

    MainThread( const orcaice::Context& context );

    // from SubsystemThread
    virtual void walk();

private:
    
    void initNetwork();

    orca::PathPlanner2dData planPath( orca::PathPlanner2dTask &task );

    // required interface to pathplanner
    orca::PathPlanner2dPrx pathplanner2dPrx_;

    // receives and stores information about computed paths 
    orcaifaceimpl::StoringPathPlanner2dConsumerImplPtr computedPathConsumer_;
        
    // If the path planner takes more than this amount of time, assume something's wrong.
    double pathPlanTimeout_;
    
    orcaice::Context context_;

};

} // namespace

#endif
