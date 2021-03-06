#ifndef TALKER_GOALEVALUATOR_ICE
#define TALKER_GOALEVALUATOR_ICE

#include <orca/common.ice>
#include <orca/bros1.ice>

#include <orca/pathfollower2d.ice> // for waypoints and stuff

module talker
{
/*!
    @ingroup ep_interfaces
    @defgroup ep_interface_goalevaluator GoalEvaluator
    @brief GoalEvaluator interface for 2d tasks.

    @{
*/

//! Task along the path.
struct Task2d {
	//! TODO: should also include time or order-constraints (e.g. reference
	//! to another task if this has to be executed after the other)
	
	//! where we have to go
	orca::Frame2d 		target;
};

//! List of path tasks
sequence<Task2d>	TaskList2d;

struct Bundle2d {
	//! total cost of this bundle - MUST come first here for proper < sorting
	float				cost;
	//! ordered list of tasks in this bundle
	TaskList2d 			tasks;
};

//! List of bundles
sequence<Bundle2d> 	BundleList2d;

struct GoalEvaluatorResult {
	string				id;
	BundleList2d	data;
};

/*!
    @brief Consumer of a planned path
*/
interface GoalEvaluatorConsumer {
    //! Transmits _all_ computed paths to the consumer
    void setData( GoalEvaluatorResult obj );
};

struct GoalEvaluatorTask {
	string			id;
	string			sender;
	//! maximum number of bundles to be computed
	int					maxBundles;
	//! maximum size of subset of tasks to be considered
	int					bundleSize;
	//! new tasks the agent might consider adding
	TaskList2d			newTasks;
	//! tasks the agent has already committed himself to
	TaskList2d			committedTasks;
	//! starting point of all paths
	orca::Frame2d		start;
	//! Consumer proxy
	GoalEvaluatorConsumer* prx;
};


//! Interface for path evaluator.
interface GoalEvaluator
{
    //! Set a task.
    //! Returns the number of tasks currently in the queue (not including the one which was just set).
    int setTask( GoalEvaluatorTask task )
            throws orca::BusyException, orca::RequiredInterfaceFailedException;   

    //! Returns the most-recently-computed computed path
    ["cpp:const"] idempotent GoalEvaluatorResult getData(string sender);

    /*!
     * Mimics IceStorm's subscribe().  The implementation may choose to
     * implement the push directly or use IceStorm.  This choice is transparent to the subscriber.
     * @param subscriber The subscriber's proxy.
     * @see unsubscribe
     */
    void subscribe( GoalEvaluatorConsumer *subscriber )
            throws orca::SubscriptionFailedException;

    /**
     * Unsubscribe the given [subscriber].
     * @param subscriber The proxy of an existing subscriber.
     * @see subscribe
    **/
    idempotent void unsubscribe( GoalEvaluatorConsumer *subscriber );
};

/*! @} */
}; // module

#endif
