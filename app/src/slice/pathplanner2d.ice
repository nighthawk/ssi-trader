/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_PATHPLANNER_2D_INTERFACE_ICE
#define ORCA2_PATHPLANNER_2D_INTERFACE_ICE

#include <orca/bros1.ice>
#include <orca/pathfollower2d.ice>

module orca
{
/*!
    @ingroup orca_interfaces
    @defgroup orca_interface_pathplanner2d PathPlanner2d
    @brief PathPlanner interface for 2D world
    @{
*/


//! Error codes for results of planning a path
enum PathPlanner2dResult 
{ 
    PathOk, 
    PathStartNotValid, 
    PathDestinationNotValid, 
    PathDestinationUnreachable,
    PathOtherError
};

//! Data structure for resulting path including error codes
struct PathPlanner2dData
{
    //! Time when data was measured.
    Time timeStamp;
    //! Path
    Path2d path;
    //! Result of path-planning
    PathPlanner2dResult result;
    //! Description of result (e.g. reason for failure)
    string resultDescription;
};

/*!
    @brief Consumer of a planned path
*/
interface PathPlanner2dConsumer
{
    //! Transmits _all_ computed paths to the consumer
    void setData( PathPlanner2dData obj );
};

//! 
//! Data structure holding a path planning task in
//! form of a coarse path (first entry is the starting point) and a 
//! proxy that receives the computed fine-grained path.
//!
struct PathPlanner2dTask
{
    //! Time when data was measured.
    Time timeStamp;
    //! Coarse path
    Path2d coarsePath;
    //! Consumer proxy
    PathPlanner2dConsumer* prx;
};

/*!
    @brief Planning a path in 2D

    PathPlanner is an interface that accepts a task consisting of a
    coarse path with the first entry being the starting waypoint. It
    serves the computed fine-grained path to the consumer via
    proxy. "setTask" returns the number of tasks in queue ahead of this new task.

    The second method of accessing the computed path is to use the
    getData method. The third method is via subscribe. These two
    methods are used when a component wants direct access to the
    currently computed path rather than a one-shot task-specific
    result (e.g. a GUI).
*/
interface PathPlanner2d
{
    //! Set a task.
    //! Returns the number of tasks currently in the queue (not including the one which was just set).
    int setTask( PathPlanner2dTask task )
            throws BusyException, RequiredInterfaceFailedException;   

    //! Returns the most-recently-computed computed path
    ["cpp:const"] idempotent PathPlanner2dData getData();

    /*!
     *
     * Mimics IceStorm's subscribe().  The implementation may choose to
     * implement the push directly or use IceStorm.  This choice is transparent to the subscriber.
     *
     * @param subscriber The subscriber's proxy.
     *
     * @see unsubscribe
     *
     */
    void subscribe( PathPlanner2dConsumer *subscriber )
            throws SubscriptionFailedException;

    /**
     *
     * Unsubscribe the given [subscriber].
     *
     * @param subscriber The proxy of an existing subscriber.
     *
     * @see subscribe
     *
    **/
    idempotent void unsubscribe( PathPlanner2dConsumer *subscriber );

};

//! @}
}; // module

#endif
