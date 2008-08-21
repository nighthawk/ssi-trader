/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#include <orcaice/orcaice.h>
#include <orcaifacestring/pathplanner2d.h>
#include <orcaobj/orcaobj.h>

#include "mainthread.h"


using namespace std;

namespace pathevaluator {

namespace {

    // Exceptions thrown/caught internally.
    // If isTemporary we'll hopefully be able to recover soon.
    class GoalPlanException : public std::exception
    { 
    public:
        GoalPlanException(const char *message, bool isTemporary)
            : message_(message),
              isTemporary_(isTemporary)
            {}
        GoalPlanException(const std::string &message, bool isTemporary)
            : message_(message),
              isTemporary_(isTemporary)
            {}
        ~GoalPlanException()throw(){}
        virtual const char* what() const throw() { return message_.c_str(); }
        bool isTemporary() const throw() { return isTemporary_; }
    private:
        std::string  message_;
        bool isTemporary_;
    };

    // Computes waypoint to start from
    // This is for the pathplanner's purposes, not the pathfollower.
    // So we don't care too much about tolerances and speeds.
    orca::Waypoint2d computeFirstWaypointForPathPlanning( const hydronavutil::Pose &pose )
    {
        orca::Waypoint2d wp;

        wp.target.p.x = pose.x();
        wp.target.p.y = pose.y();
        wp.target.o   = pose.theta();

        // add bogus tolerances and speeds
        wp.distanceTolerance = (Ice::Float)0.1;
        wp.headingTolerance  = (Ice::Float)(M_PI/2.0);
        wp.timeTarget.seconds  = 0;
        wp.timeTarget.useconds = 0;
        wp.maxApproachSpeed    = 2000;
        wp.maxApproachTurnrate = (float)DEG2RAD(2000); 

        return wp;
    }

    // TODO: remove?
    double ageOf( const orca::Time &ts )
    {
        return orcaice::timeDiffAsDouble( orcaice::getNow(), ts );
    }

    double distance( const hydronavutil::Pose &pose, const orca::Waypoint2d &wp )
    {
        return hypot( pose.y()-wp.target.p.y, pose.x()-wp.target.p.x );
    }
    double distance( const orca::Waypoint2d &wp1, const orca::Waypoint2d &wp2 )
    {
        return hypot( wp1.target.p.y-wp2.target.p.y, wp1.target.p.x-wp2.target.p.x );
    }
}

MainThread::MainThread( const orcaice::Context & context )
    : orcaice::SubsystemThread( context.tracer(), context.status(), "MainThread" ),
      context_(context)
{
    subStatus().setMaxHeartbeatInterval( 10.0 );

    Ice::PropertiesPtr prop = context_.properties();
    std::string prefix = context_.tag()+".Config.";
    pathPlanTimeout_ = orcaice::getPropertyAsDoubleWithDefault( prop, prefix+"PathPlanTimeout", 10.0 );
    velocityToFirstWaypoint_ = orcaice::getPropertyAsDoubleWithDefault( prop, prefix+"VelocityToFirstWaypoint", 1.0 );
}

void
MainThread::initNetwork()
{
    //
    // ENABLE NETWORK CONNECTIONS
    //
    // multi-try function
    orcaice::activate( context_, this, subsysName() );

    //
    // REQUIRED INTERFACES: Pathplanner
    //

    subStatus().initialising( "Connecting to PathPlanner2d" );
    orcaice::connectToInterfaceWithTag( context_,
                                        pathplanner2dPrx_,
                                        "PathPlanner2d",
                                        this,
                                        subStatus().name() );

    // Create consumer to receive planned paths from the path-planner
    computedPathConsumer_ = new orcaifaceimpl::StoringPathPlanner2dConsumerImpl( context_ );

/*    
    //
    // PROVIDED INTERFACES: pathevaluator2d
    //
    
    // create the proxy/buffer for incoming path
    incomingPathI_ = new PathFollower2dI( incomingPathStore_,
                                          activationStore_,
                                          localNavPrx_ );
    
    Ice::ObjectPtr pathFollowerObj = incomingPathI_;

    // two possible exceptions will kill it here, that's what we want
    orcaice::createInterfaceWithTag( context_, pathFollowerObj, "PathFollower2d" );
*/
}

orca::PathPlanner2dData
MainThread::planPath( const hydronavutil::Pose &pose, 
                    const orca::PathFollower2dData &coarsePath )
{
    // put together a task for the pathplanner
    // add the position of the robot as the first waypoint in the path
    orca::Waypoint2d firstWp = computeFirstWaypointForPathPlanning(pose);
    orca::PathPlanner2dTask task;
    task.timeStamp  = coarsePath.timeStamp;
    task.coarsePath = coarsePath.path;
    task.coarsePath.insert( task.coarsePath.begin(), 1, firstWp );
    task.prx = computedPathConsumer_->consumerPrx();

    // send task to pathplanner
    stringstream ssSend;
    ssSend << "MainThread::"<<__func__<<": Sending task to pathplanner: " << orcaobj::toVerboseString( task );
    context_.tracer().debug(ssSend.str());
    int numJobsAheadInQueue = pathplanner2dPrx_->setTask( task );
    if ( numJobsAheadInQueue > 1 )
    {
        stringstream ss;
        ss << "MainThread::planPath(): path planner is busy, there are " << numJobsAheadInQueue << " ahead of us in the queue.";
        context_.tracer().warning( ss.str() );
    }
            
    // block until path is computed
    context_.tracer().debug("MainThread: Waiting for pathplanner's answer");
    orca::PathPlanner2dData computedPath;
    // (need a loop here so ctrlC works)
    int secWaited=0;
    while ( !isStopping() )
    {
        int ret = computedPathConsumer_->store().getNext( computedPath, 1000 );
        if ( ret == 0 )
            break;
        else
        {
            if ( ++secWaited > pathPlanTimeout_ )
            {
                stringstream ss;
                ss << "Did not receive a reply from the PathPlanner, after waiting " << secWaited << "s -- something must be wrong.";
                const bool isTemporary = false;
                throw( GoalPlanException( ss.str(), isTemporary ) );
            }
        }
    }
            
    // check result
    if ( computedPath.result != orca::PathOk )
    {
        stringstream ss;
        ss << "MainThread: PathPlanner could not compute.  Gave result " 
           << ifacestring::toString( computedPath.result )<<": "<<computedPath.resultDescription;
        const bool isTemporary = true;
        throw( GoalPlanException( ss.str(), isTemporary ) );
    }

    assert( computedPath.path.size() > 0 );
    return computedPath;
}


void 
MainThread::walk()
{
    initNetwork();

    subStatus().setMaxHeartbeatInterval( 3.0 );

    // main loop
    while ( !isStopping() )
    {
        try
        {
            context_.tracer().info("Creating new goal path");
            
            // TODO: CHANGE THIS => setup pose and path
            /*
            const hydronavutil::Pose pose = new ...
            

            stringstream ssPath;
            ssPath << "MainThread: Received path request: " << endl << orcaobj::toVerboseString(path);
            context_.tracer().debug( ssPath.str() );

            orca::PathPlanner2dData  plannedPath = planPath( pose, path );
						*/

            subStatus().ok();

        } // try
        catch ( const GoalPlanException &e )
        {
            stringstream ss;
            if ( e.isTemporary() )
            {
                ss << "MainThread:: Caught GoalPlanException: " << e.what() << ".  I reckon I can recover from this.";
                context_.tracer().warning( ss.str() );
                subStatus().warning( ss.str() );

                // Slow the loop down a little before trying again.
                IceUtil::ThreadControl::sleep(IceUtil::Time::seconds(1));
            }
            else
            {
                ss << "MainThread:: Caught GoalPlanException: " << e.what() << ".  Looks unrecoverable, I'm giving up.";
                context_.tracer().error( ss.str() );
                subStatus().fault( ss.str() );
            }
        }
        catch ( const Ice::Exception & e )
        {
            stringstream ss;
            ss << "MainThread:: Caught exception: " << e;
            context_.tracer().error( ss.str() );
            subStatus().fault( ss.str() );
        }
        catch ( const std::exception & e )
        {
            stringstream ss;
            ss << "MainThread: Caught exception: " << e.what();
            context_.tracer().error( ss.str() );
            subStatus().fault( ss.str() );
        }
        catch ( ... )
        {
            context_.tracer().error( "MainThread: caught unknown unexpected exception.");
            subStatus().fault( "MainThread: caught unknown unexpected exception.");
        }
            
    } // end of big while loop
    
    // wait for the component to realize that we are quitting and tell us to stop.
    waitForStop();
}

}
