/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#include <algorithm>

#include <orcaice/orcaice.h>
#include <orcaifacestring/pathplanner2d.h>
#include <orcaobj/orcaobj.h>

#include "mainthread.h"
#include "combination.h"


using namespace std;

namespace pathevaluator {

namespace {

    // Exceptions thrown/caught internally.
    // If isTemporary we'll hopefully be able to recover soon.
    class PlanEvalException : public std::exception
    { 
    public:
        PlanEvalException(const char *message, bool isTemporary)
            : message_(message),
              isTemporary_(isTemporary)
            {}
        PlanEvalException(const std::string &message, bool isTemporary)
            : message_(message),
              isTemporary_(isTemporary)
            {}
        ~PlanEvalException()throw(){}
        virtual const char* what() const throw() { return message_.c_str(); }
        bool isTemporary() const throw() { return isTemporary_; }
    private:
        std::string  message_;
        bool isTemporary_;
    };

    // Computes waypoint
    // This is for the pathplanner's purposes, not the pathfollower.
    // So we don't care too much about tolerances and speeds.
    orca::Waypoint2d createWaypoint( double x, double y, double theta )
    {
        orca::Waypoint2d wp;

        wp.target.p.x = x;
        wp.target.p.y = y;
        wp.target.o   = theta;

        // add bogus tolerances and speeds
        wp.distanceTolerance = (Ice::Float)0.1;
        wp.headingTolerance  = (Ice::Float)(M_PI/2.0);
        wp.timeTarget.seconds  = 0;
        wp.timeTarget.useconds = 0;
        wp.maxApproachSpeed    = 2000;
        wp.maxApproachTurnrate = (float)DEG2RAD(2000); 

        return wp;
    }
    
    orca::Waypoint2d createWaypoint( talker::PathTask2d task )
    {
        orca::Waypoint2d wp;

        wp.target = task.target;

        // add bogus tolerances and speeds
        wp.distanceTolerance = (Ice::Float)0.1;
        wp.headingTolerance  = (Ice::Float)(M_PI/2.0);
        wp.timeTarget.seconds  = 0;
        wp.timeTarget.useconds = 0;
        wp.maxApproachSpeed    = 2000;
        wp.maxApproachTurnrate = (float)DEG2RAD(2000); 

        return wp;
    }

    
    // Creates a task along the path which is to be evaluated
    talker::PathTask2d createTask( double x, double y, double theta) {
    	talker::PathTask2d task;
    	task.target.p.x = x;
    	task.target.p.y = y;
    	task.target.o   = theta;
    	return task;
    }
    
    // TODO: put somewhere else
    

    double distance( const hydronavutil::Pose &pose, const orca::Waypoint2d &wp )
    {
        return hypot( pose.y()-wp.target.p.y, pose.x()-wp.target.p.x );
    }
    double distance( const orca::Waypoint2d &wp1, const orca::Waypoint2d &wp2 )
    {
        return hypot( wp1.target.p.y-wp2.target.p.y, wp1.target.p.x-wp2.target.p.x );
    }
    
    int fac(int n)
		{
			if(n<2)return(1);
			return((n)*fac(n-1));
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
}

void
MainThread::initNetwork()
{
    // ENABLE NETWORK CONNECTIONS
    //
    // multi-try function
    orcaice::activate( context_, this, subsysName() );

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

float
MainThread::computePathCost( talker::PathTask2d start, talker::TaskList2d &tasks )
{
		// put together a task for the pathplanner
		orca::PathPlanner2dTask task;
		task.coarsePath.push_back(createWaypoint( start ));

		for(talker::TaskList2d::iterator it = tasks.begin(); it != tasks.end(); ++it)
			task.coarsePath.push_back(createWaypoint( *it ));

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
                throw( PlanEvalException( ss.str(), isTemporary ) );
            }
        }
    }
            
    // check result
    if ( computedPath.result != orca::PathOk )
    {
        stringstream ss;
        ss << "MainThread: PathPlanner could not compute. Gave result " 
           << ifacestring::toString( computedPath.result )<<": "<<computedPath.resultDescription;
        const bool isTemporary = true;
        throw( PlanEvalException( ss.str(), isTemporary ) );
    }
    assert( computedPath.path.size() > 0 );
    
    // compute distance
		float dist = 0.;
		orca::Waypoint2d last = computedPath.path[0];
		for (int i = 1; i < computedPath.path.size(); i++) {
			orca::Waypoint2d current = computedPath.path[i];
			dist += distance(last, current);
		
			last = current;
		}

    return dist;
}


void
MainThread::filterTasks( talker::PathTask2d &cur, 
												 talker::TaskList2d &tasks,
												 int bundleSize,
												 int maxBundles )
{
	// TODO: implement
}


talker::BundleList2d
MainThread::findBundles( talker::PathTask2d &start, 
												 talker::TaskList2d &committed, 
												 talker::TaskList2d &tasks,
												 int bundleSize,
												 int maxBundles )
{
	talker::BundleList2d bundles;

	// for all bundle sizes less than the max
	for(int k = 1; k <= bundleSize; k++)
	{
		// create starting subset
		talker::TaskList2d subset(k);
		for(int i = 0; i < k; i++)
		{
			subset.at(i) = tasks.at(i);
		}
	
		// iterate of k-subsets
		do
		{
			talker::TaskList2d permut(k);
			permut = subset;
			
			// add tasks that the agent already committed to
			for(talker::TaskList2d::iterator it = committed.begin();it!=committed.end();++it)
			{
				talker::PathTask2d t = *it;
				permut.push_back(t);
			}

			// iterate over permutations of this subset
			for(int j = 0; j < fac(k + committed.size()); j++)
			{
				// find next permutation
  			next_permutation(permut.begin(), permut.end());

				// create bundle and evaluate

				talker::TaskBundle2d bundle;
				bundle.tasks = permut;
				bundle.cost  = computePathCost(start, permut);

				for(talker::TaskList2d::iterator it = permut.begin();it!=permut.end();++it)
				{
					talker::PathTask2d t = *it;
					cout << t.target.p.x << " " << t.target.p.y << " | ";
				}
				cout << bundle.cost << endl;  

			}
		}
		while( stdcomb::next_combination(tasks.begin(),	tasks.end(),
																		 subset.begin(),subset.end()) );
	}			
	
	return bundles;
}

void 
MainThread::walk()
{
    initNetwork();

    // main loop
    while ( !isStopping() )
    {
        try
        {
            context_.tracer().info("Waiting for tasks.");
        		
        		// wait for new set of tasks
        		
        		// TODO - just faked here
        		talker::TaskList2d tasks;
						tasks.push_back(createTask( -9.,  0., 0.));
        		tasks.push_back(createTask( -1.,  3., 0.));
						tasks.push_back(createTask( 23., -4., 0.));
						tasks.push_back(createTask(-23.,  8., 0.));
						
						talker::PathTask2d start = createTask( -1.,  0., 0.);

        		talker::TaskList2d committed;
						committed.push_back(createTask( 11.,  0., 0.));
						
						int maxBundles = 3;
						int bundleSize = 3;
						
            stringstream ssPath;
            ssPath << "MainThread: Received " << tasks.size() << " tasks." << endl;
            context_.tracer().debug( ssPath.str() );

						// filter tasks
						filterTasks(start, tasks, bundleSize, maxBundles);
						
						// find best bundles
						talker::BundleList2d bundles;
						bundles = findBundles(start, committed, tasks, bundleSize, maxBundles);

            context_.tracer().info("Done processing bundles.");
            
            subStatus().ok();

        } // try
        catch ( const PlanEvalException &e )
        {
            stringstream ss;
            if ( e.isTemporary() )
            {
                ss << "MainThread:: Caught PlanEvalException: " << e.what() << ".  I reckon I can recover from this.";
                context_.tracer().warning( ss.str() );
                subStatus().warning( ss.str() );

                // Slow the loop down a little before trying again.
                IceUtil::ThreadControl::sleep(IceUtil::Time::seconds(1));
            }
            else
            {
                ss << "MainThread:: Caught PlanEvalException: " << e.what() << ".  Looks unrecoverable, I'm giving up.";
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
