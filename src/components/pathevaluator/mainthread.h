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
#include <gbxsickacfr/gbxiceutilacfr/store.h>
#include <hydronavutil/pose.h>
#include <orca/pathplanner2d.h>
#include <hydroogmap/hydroogmap.h>
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

    orca::PathPlanner2dData planPath( const hydronavutil::Pose &pose, const orca::PathFollower2dData &coarsePath );

    // Adjust timing: work out how long it takes to the first waypoint based on straight-line distance 
    // and configured velocityToFirstWaypoint_. Take the max of first wp time and the computed time.
    // Add this time to all waypoints.
    void addTimeToReachFirstWp( const hydronavutil::Pose &pose,
                                orca::PathFollower2dData &incomingPath );
    
    // required interface to pathplanner
    orca::PathPlanner2dPrx pathplanner2dPrx_;

    // receives and stores information about computed paths 
    orcaifaceimpl::StoringPathPlanner2dConsumerImplPtr computedPathConsumer_;
        
    // If the path planner takes more than this amount of time, assume something's wrong.
    double pathPlanTimeout_;
    
    // Velocity to get to the first waypoint
    double velocityToFirstWaypoint_;
    orcaice::Context context_;

};

} // namespace

#endif
