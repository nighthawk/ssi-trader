/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_PATH_FOLLOWER_INTERFACE_ICE
#define ORCA2_PATH_FOLLOWER_INTERFACE_ICE

#include <orca/common.ice>
#include <orca/bros1.ice>

module orca
{
/*!
    @ingroup orca_interfaces
    @defgroup orca_interface_pathfollower2d PathFollower2d
    @brief Interface for a component that can follow a 2d path, defined as a set of waypoints

The Pathfollower2d interface.

    @{
*/

//! A single waypoint
struct Waypoint2d {

    //! The pose of the waypoint
    Frame2d target;

    //! How close the follower has to get to the target
    float   distanceTolerance;

    //! How closely the follower has to match the target orientation
    float   headingTolerance;

    //! The time at which the follower should reach the waypoint, as
    //! an offset from the time the path was activated.
    Time    timeTarget;

    //! Max speed at which the follower can approach this waypoint
    float   maxApproachSpeed;

    //! Max turnrate the follower can use when approaching this waypoint
    float   maxApproachTurnrate;
};

//! A sequence of waypoints
sequence<Waypoint2d>        Path2d;

/*!
    WaypointFollower data structure
*/
struct PathFollower2dData
{
    //! Time when data was measured.
    Time timeStamp;

    //! The path
    Path2d              path;
};

/*!
    @brief Consumer of path updates
*/
interface PathFollower2dConsumer
{
    //! Used by the follower to broadcast changes without transmitting the entire path.
    //!   Given that the follower is loaded up with a list of n waypoints,
    //!   the follower will call 'setWaypointIndex' with the index
    //!   of the waypoint it is _actively_ seeking.
    //!   - Normal waypoint indices are in the range [0,n-1]
    //!   - The special index '-1' means 'I just completed my path and became inactive'.
    //!   - Suppose the follower has been loaded with a path but is inactive.  
    //!     On activation, it will call 'setWaypointIndex(0)'.
    idempotent void setWaypointIndex( int index );
    
    //! Once the follower starts following a path it notifies consumers of the activation time. 
    //! It provides:
    //! - the absolute time (useful if clocks of provider and consumers are synchronized).
    //! - the relative time (how many seconds ago the follower started following).
    idempotent void setActivationTime( Time absoluteTime, double relativeTime );

    //! When the follower is loaded with a new path, it transmits that path to
    //! all subscribers.
    idempotent void setData( PathFollower2dData data );
    
    //! When the follower's enabled-state changes, it tells all subscribers about it
    idempotent void setEnabledState( bool enabledState );
};

/*!
    @brief The Path Follower

    To make the follower follow a path:

    - either (a)
        -# call setData(thePath,false) to load the path, then
        -# call activateNow() to make it go.

    - or (b)
        -# call setData(thePath,true) to load the path and have the
         follower start following it immediately
 */
interface PathFollower2d
{
    //! Returns the path currently being followed, or throws DataNotExistException
    //! if none has been set.
    ["cpp:const"] idempotent PathFollower2dData getData();
            
    //! Loads the follower with a path to follow.
    //! If @c activateImmediately is false, the follower won't go anywhere
    //! until activateNow() is called.
    //!
    //! Loading the follower with a new path while it is active
    //! over-writes the old path.  The activation status of the follower is
    //! set to the value of @c activateImmediately.
    //!
    //! The follower can therefore be stopped by calling setData(emptyPath,false).
    //!
    idempotent void setData( PathFollower2dData path, bool activateImmediately )
        throws MalformedParametersException, OrcaException;

    //! Start following the previously-loaded path.
    //! alexm: what happens when none is loaded? Silently ignored?
    idempotent void activateNow();

    //! Returns the index of the waypoint which is currently pursued:
    //!   - '-1'    : none (inactive)
    //!   - [0,n-1] : an index into the list of n waypoints.
    ["cpp:const"] idempotent int getWaypointIndex();

    //! If the PathFollower is active (alexm: what does that mean? has path?)
    //!   - returns TRUE and
    //!   - sets the absolute time when the follower started following the current path
    //! If the PathFollower is inactive:
    //!   - returns FALSE and
    //!   - does not set the activation time
    ["cpp:const"] idempotent bool getAbsoluteActivationTime( out Time activationTime );
    
    //! If the PathFollower is active:
    //!   - returns true and
    //!     sets secondsSinceActivation to the number of seconds since
    //!     the follower started following the current path
    //! If the PathFollower is inactive:
    //!   - returns false and
    //!     does not set secondsSinceActivation
    ["cpp:const"] idempotent bool getRelativeActivationTime( out double secondsSinceActivation );

    //! Disabling the PathFollower stops it from sending any commands to the 
    //! robot.  It says, "Take your hands off the wheel!".  No commands can
    //! be send until it is re-enabled.
    idempotent void setEnabled( bool enabled );

    //! Get the enabled/disabled state.
    ["cpp:const"] idempotent bool enabled();
    
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
    void subscribe( PathFollower2dConsumer *subscriber )
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
    idempotent void unsubscribe( PathFollower2dConsumer *subscriber );
};

/*! @} */
}; // module

#endif
