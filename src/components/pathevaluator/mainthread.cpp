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

#include <algorithm>
#include <queue>
#include "combination.h"
#include "mainthread.h"

using namespace std;
using namespace talker;

namespace pathevaluator {

// private helper functions
namespace {

    // Exceptions thrown/caught internally.
    // If isTemporary we'll hopefully be able to recover soon.
    class PlanEvalException : public std::exception { 
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
    orca::Waypoint2d createWaypoint( double x, double y, double theta ) {
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
    orca::Waypoint2d createWaypoint( Task2d task ) {
    	return createWaypoint( task.target.p.x, task.target.p.y, task.target.o );
    }

    // Creates a task along the path which is to be evaluated
    Task2d createTask( double x, double y, double theta) {
    	Task2d task;
    	task.target.p.x = x;
    	task.target.p.y = y;
    	task.target.o   = theta;
    	return task;
    }
    
    double distance( const hydronavutil::Pose &pose, const orca::Waypoint2d &wp ) {
        return hypot( pose.y()-wp.target.p.y, pose.x()-wp.target.p.x );
    }
    double distance( const orca::Waypoint2d &wp1, const orca::Waypoint2d &wp2 ) {
        return hypot( wp1.target.p.y-wp2.target.p.y, wp1.target.p.x-wp2.target.p.x );
    }
    
    int fac(int n) {
			if (n<2)
				return (1);
			else
				return ((n)*fac(n-1));
		}
}

MainThread::MainThread( const orcaice::Context & context )
    : orcaice::SubsystemThread( context.tracer(), context.status(), "MainThread" ),
			pathEvaluatorTaskBuffer_( 100, gbxiceutilacfr::BufferTypeQueue),
      context_(context)
{
    subStatus().setMaxHeartbeatInterval( 10.0 );

    Ice::PropertiesPtr prop = context_.properties();
    std::string prefix = context_.tag()+".Config.";
    pathPlanTimeout_ = orcaice::getPropertyAsDoubleWithDefault( prop, prefix+"PathPlanTimeout", 10.0 );
}

void
MainThread::initNetwork() {
    // REQUIRED INTERFACES: Pathplanner
    subStatus().initialising( "Connecting to PathPlanner2d" );
    orcaice::connectToInterfaceWithTag( context_,
                                        pathplanner2dPrx_,
                                        "PathPlanner2d",
                                        this,
                                        subStatus().name() );

    // Create consumer to receive planned paths from the path-planner
    computedPathConsumer_ = new orcaifaceimpl::StoringPathPlanner2dConsumerImpl( context_ );


    // PROVIDED INTERFACES: pathevaluator2d
    
    // create the proxy/buffer for incoming tasks
    subStatus().initialising("Creating PathEvaluator Interface" );
    pathEvaluatorI_ = new PathEvaluatorI( pathEvaluatorTaskBuffer_, context_ );
    Ice::ObjectPtr pathEvaluatorObj = pathEvaluatorI_;

    // two possible exceptions will kill it here, that's what we want
    orcaice::createInterfaceWithTag( context_, pathEvaluatorObj, "PathEvaluator" );
}

float
MainThread::computePathCost( Task2d start, TaskList2d &tasks )
{
		// put together a task for the pathplanner
		orca::PathPlanner2dTask task;
		task.coarsePath.push_back(createWaypoint( start ));

		for(TaskList2d::iterator it = tasks.begin(); it != tasks.end(); ++it)
			task.coarsePath.push_back(createWaypoint( *it ));

		task.prx = computedPathConsumer_->consumerPrx();


    // send task to pathplanner
    stringstream ssSend;
    ssSend << "MainThread::"<<__func__<<": Sending task to pathplanner: " << orcaobj::toVerboseString( task );
    context_.tracer().debug(ssSend.str());
    int numJobsAheadInQueue = pathplanner2dPrx_->setTask( task );
    if ( numJobsAheadInQueue > 1 ) {
        stringstream ss;
        ss << "MainThread::computePathCost: path planner is busy, there are " << numJobsAheadInQueue << " ahead of us in the queue.";
        context_.tracer().warning( ss.str() );
    }
            
    // block until path is computed
    context_.tracer().debug("MainThread: Waiting for pathplanner's answer");
    orca::PathPlanner2dData computedPath;
    // (need a loop here so ctrlC works)
    int secWaited=0;
    while ( !isStopping() ) {
        int ret = computedPathConsumer_->store().getNext( computedPath, 1000 );
        if ( ret == 0 )
            break;
        else {
            if ( ++secWaited > pathPlanTimeout_ ) {
                stringstream ss;
                ss << "MainThread::computePathCost: Did not receive a reply from the PathPlanner, after waiting " << secWaited << "s -- something must be wrong.";
                const bool isTemporary = false;
                throw( PlanEvalException( ss.str(), isTemporary ) );
            }
        }
    }
            
    // check result
    if ( computedPath.result != orca::PathOk ) {
        stringstream ss;
        ss << "MainThread::computePathCost: PathPlanner could not compute. Gave result " 
           << ifacestring::toString( computedPath.result )<<": "<<computedPath.resultDescription;
        const bool isTemporary = true;
        throw( PlanEvalException( ss.str(), isTemporary ) );
    }
    assert( computedPath.path.size() > 0 );
    
    // compute distance
		float dist = 0.;
		orca::Waypoint2d last = computedPath.path[0];
		for (unsigned int i = 1; i < computedPath.path.size(); i++) {
			orca::Waypoint2d current = computedPath.path[i];
			dist += distance(last, current);
		
			last = current;
		}

    return dist;
}


void
MainThread::filterTasks( Task2d &cur, 
												 TaskList2d &tasks,
												 unsigned int bundleSize,
												 unsigned int maxBundles ) 
{
	// TODO: implement
}

BundleList2d
MainThread::findBundles( Task2d &start, 
												 TaskList2d &committed, 
												 TaskList2d &tasks,
												 unsigned int bundleSize,
												 unsigned int maxBundles ) 
{
	bool permutate_committed = false;
	
	// queue storing all computed bundles sorted by cost
	priority_queue<Bundle2d, BundleList2d, greater<BundleList2d::value_type> > queue;

	// if bundleSize is greater than the actual size of tasks, only create
	// subsets up to the size of the tasks
	// if (tasks.size() < bundleSize) bundleSize = tasks.size();
	
	// for all bundle sizes less than the max
	for(unsigned int k = 1; k <= bundleSize; k++) {
		
		unsigned int size = k;
		if (size > tasks.size()) size = tasks.size();
		
		// create starting subset
		TaskList2d subset(size);
		for(unsigned int i = 0; i < size; i++)
			subset.at(i) = tasks.at(i);
	
		// iterate over k-subsets
		do {
			TaskList2d permut(size);
			permut = subset;
			
			// add tasks that the agent already committed to for inclusion
			// in permutations if necessary
			if (permutate_committed) {
				for(TaskList2d::iterator it = committed.begin();it!=committed.end();++it) {
					Task2d t = *it;
					permut.push_back(t);
				}
			}
			
			int combinations;
			if (permutate_committed)
				combinations = fac(permut.size());
			else
				combinations = fac(committed.size() + permut.size()) / fac(committed.size());
			
			for (int magic = 1; magic <= combinations; magic++) {
				TaskList2d result;
				if (!permutate_committed)
					result = committed;
				
				// add all tasks to result
				// this adds all tasks to any possible position within _committed_
				// tasks, keeping committed in fixed order and using all possible
				// permutations of _tasks_
				int n = result.size();
				for (unsigned int i = 0; i < permut.size(); i++) {
					int at = magic % (n + i + 1);
					
					TaskList2d::iterator it = result.begin();
					result.insert(it + at, permut.at(i));
				}
				
				Bundle2d bundle;
				bundle.tasks = result;
				bundle.cost  = computePathCost(start, bundle.tasks);

				queue.push(bundle);

        stringstream ss;
        ss << "MainThread::findBundles: ";
				for(TaskList2d::iterator it = result.begin(); it!=result.end(); ++it) {
					Task2d t = *it;
					ss << t.target.p.x << " " << t.target.p.y << " | ";
				}
				ss << bundle.cost;
        context_.tracer().debug( ss.str() );
			}

			/*
			// iterate over permutations of this subset, actual number of permutations
			// has to be calculated by hand (fac...) as next_permutation might stop
			// prematurely if we use a while(next_permutation...) loop
			for(unsigned int j = 0; j < (unsigned int) fac(permut.size()); j++)
			{
  			next_permutation(permut.begin(), permut.end());

				// create bundle and evaluate
				Bundle2d bundle;
				bundle.tasks = permut;
				bundle.cost  = computePathCost(start, bundle.tasks);
			
				queue.push(bundle);

        stringstream ss;
        ss << "MainThread::findBundles: ";
				for(TaskList2d::iterator it = permut.begin();it!=permut.end();++it)
				{
					Task2d t = *it;
					ss << t.target.p.x << " " << t.target.p.y << " | ";
				}
				ss << bundle.cost << endl;
        context_.tracer().debug( ss.str() );

			}
			*/
		}
		while( tasks.size() > 0 && subset.size() > 0 && stdcomb::next_combination(tasks.begin(),	tasks.end(), subset.begin(),subset.end()) );
	}			

	// return the cheapest $maxBundles bundles
	BundleList2d bundles;
	if (maxBundles > queue.size()) maxBundles = queue.size();
	for (unsigned int i = 0; i < maxBundles; i++) {
		bundles.push_back(queue.top());
		queue.pop();
	}
	return bundles;
}

BundleList2d
MainThread::createBundles( PathEvaluatorTask task ) {
	// FIXME: start should actually always just be a frame2d
	Task2d start = createTask( task.start.p.x, task.start.p.y, task.start.o);

  // plan pseudo path to overcome problem of first path being screwed up
  // FIXME: there must be a better way to handle this
  context_.tracer().info("MainThread::createBundles: Flushing path planner with arbitrary task.");
	TaskList2d empty;
	findBundles(start, empty, empty, 1, 0);
  context_.tracer().info("MainThread::createBundles: Done flushing path planner.");

	// filter tasks
	filterTasks(start, task.newTasks, task.bundleSize, task.maxBundles);
	
	// find best bundles
	BundleList2d bundles;
	bundles = findBundles(start, task.committedTasks, task.newTasks, task.bundleSize, task.maxBundles);	
	
	return bundles;
}

void 
MainThread::walk()
{
  	orcaice::activate( context_, this, subsysName() );
    
		initNetwork();
    
		PathEvaluatorTask task;

    while ( !isStopping() )
    {
        try
        {
            //
            //  ======== waiting for a task (blocking) =======
            //
            context_.tracer().info("MainThread: Waiting for a new task.");
            bool haveTask=false;

/*
        		// TODO - just faked here
        		orca::Frame2d start;
        		start.p.x = -1.;
        		start.p.y = 0.;
        		start.o		= 0.;

        		TaskList2d tasks;
						tasks.push_back(createTask( -9.,  0., 0.));
        		tasks.push_back(createTask( -1.,  3., 0.));
						tasks.push_back(createTask( 23., -4., 0.));
						
        		TaskList2d committed;
						committed.push_back(createTask( 11.,  0., 0.));
						committed.push_back(createTask(-23.,  8., 0.));
						
        		task.maxBundles 		= 10;
        		task.bundleSize 		= 1;
        		task.start 					= start;
        		task.committedTasks = committed;
        		task.newTasks 			= tasks;
*/

            while ( !isStopping() )
            {
                int timeoutMs = 1000;
                int ret=0;
                try {
                    pathEvaluatorTaskBuffer_.getAndPop( task );
                }
                catch ( const gbxutilacfr::Exception & e ) {
                    ret = pathEvaluatorTaskBuffer_.getAndPopNext( task, timeoutMs );
                }
                if ( ret == 0 ) {
                    haveTask = true;
				            stringstream ssPath;
				            ssPath << "MainThread: Received a pack with " << task.newTasks.size() << " tasks." << endl;
                    context_.tracer().info( ssPath.str() );
                    break;
                }
                subStatus().ok();
            }

            // the only way of getting out of the above loop without a task
            // is if the user pressed Ctrl+C, ie we have to quit
            if (!haveTask) break;
            //
            // ===== process tasks and compute best bundles ========
            //
						BundleList2d bundles = createBundles(task);
            context_.tracer().debug("MainThread: Done processing bundles.");


            //
            // ======= send result (including error code) ===============
            //
            context_.tracer().info("MainThread: sending off the resulting bundles.");
						PathEvaluatorResult result;
						result.id = task.id;
						result.data = bundles;
    
            // There are three methods to let other components know:
            // 1. using the proxy
            if (task.prx != 0)
                task.prx->setData( result );
            else
                context_.tracer().warning( "MainThread: task.prx was zero!" );

            // 2. and 3.: use getData or icestorm
            pathEvaluatorI_->localSetData( result );
    
            // resize the bundle data: future tasks might not compute a path successfully and we would resend the old ones
						bundles.resize(0);
    
            int numTasksWaiting = pathEvaluatorTaskBuffer_.size();
            if ( numTasksWaiting > 1 )
            {
                stringstream ss;
                ss << "MainThread: Tasks are piling up: there are " << numTasksWaiting << " in the queue.";
                subStatus().warning( ss.str() );
            }
            else
            {
                subStatus().ok();
            }            

        } // try
        catch ( const PlanEvalException &e )
        {
            stringstream ss;
            if ( e.isTemporary() )
            {
                ss << "MainThread: Caught PlanEvalException: " << e.what() << ".  I reckon I can recover from this.";
                context_.tracer().warning( ss.str() );
                subStatus().warning( ss.str() );

                // Slow the loop down a little before trying again.
                IceUtil::ThreadControl::sleep(IceUtil::Time::seconds(1));
            }
            else
            {
                ss << "MainThread: Caught PlanEvalException: " << e.what() << ".  Looks unrecoverable, I'm giving up.";
                context_.tracer().error( ss.str() );
                subStatus().fault( ss.str() );
            }
        }
        catch ( const Ice::Exception & e )
        {
            stringstream ss;
            ss << "MainThread: Caught exception: " << e;
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
