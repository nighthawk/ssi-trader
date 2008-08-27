#ifndef TALKER_PATHEVALUATOR_ICE
#define TALKER_PATHEVALUATOR_ICE

#include <orca/common.ice>
#include <orca/bros1.ice>

#include <orca/pathfollower2d.ice> // for waypoints and stuff

module talker
{
/*!
    @ingroup ep_interfaces
    @defgroup ep_interface_pathevaluator PathEvaluator
    @brief PathEvaluator interface for 2d tasks.

Ths is an example interface definition written in Slice IDL.

    @{
*/

//! Task along the path.
struct PathTask2d {
	//! TODO: should also include time or fixed-order constraints (e.g. reference
	//! to another task if this has to be executed after the other)
	
	//! where we have to go
	orca::Frame2d 		target;
};

//! List of path tasks
sequence<PathTask2d> 		TaskList2d;

struct TaskBundle2d {
	//! ordered list of tasks in this bundle
	TaskList2d 	tasks;
	
	//! total cost of this bundle
	float				cost;
};

//! List of bundles
sequence<TaskBundle2d> 	BundleList2d;


//! Interface for path evaluator.
interface PathEvaluator
{
    //! Get current state
    idempotent bool getState();

    //! Set current state
    void setState( bool state );
};

/*! @} */
}; // module

#endif
