/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_LOCALISE_2D_INTERFACE_ICE
#define ORCA2_LOCALISE_2D_INTERFACE_ICE

#include <orca/common.ice>
#include <orca/bros1.ice>
#include <orca/stochastic.ice>
#include <orca/vehicledescription.ice>

module orca
{
/*!
    @ingroup orca_interfaces
    @defgroup orca_interface_localise2d Localise2d
    @brief Multi-hypothesis planar localisation.
    @{
*/

//! A Gaussian distribution over pose space
struct Pose2dHypothesis 
{
    //! The mean of the distribution
    Frame2d      mean;
    //! The Gaussian covariance
    Covariance2d cov;
    //! The weight of this component when used as part of a mixture
    float weight;
};

//! A set of Gaussian pose hypotheses: essentially a mixture of Gaussians
sequence<Pose2dHypothesis> Pose2dHypotheses;

//! Location estimate
struct Localise2dData
{
    //! Time when data was measured.
    Time timeStamp;

    //! Localisation hypotheses.
    Pose2dHypotheses hypotheses;
};

/*!
 * Data consumer interface.
 */
interface Localise2dConsumer
{
    //! Transmits the data to the consumer.
    void setData( Localise2dData obj );
};

/*!
    @brief Access to odometry of 2d mobile robitic bases.
*/
interface Localise2d
{
    //! Returns the latest data.
    ["cpp:const"] idempotent Localise2dData getData()
            throws DataNotExistException;
    
    //! Get the geometry of the localised vehicle
    ["cpp:const"] idempotent VehicleGeometryDescription getVehicleGeometry();

    /*!
     * Mimics IceStorm's subscribe(). @p subscriber is typically a direct proxy to the consumer object.
     * The implementation may choose to implement the push directly or use IceStorm.
     * This choice is transparent to the subscriber. The case when the @p subscriber is already subscribed
     * is quietly ignored.
     *
     * @see unsubscribe
     */
    void subscribe( Localise2dConsumer* subscriber )
        throws SubscriptionFailedException;

    // for reference, this is what IceStorm's subscribe function looks like.
    //void subscribe(QoS theQoS, Object* subscriber);

    /*!
     * Unsubscribe an existing @p subscriber. The case when the @p subscriber is not subscribed
     * is quietly ignored.
     *
     * @see subscribe
     */
    idempotent void unsubscribe( Localise2dConsumer* subscriber );
};


//!  //@}
}; // module

#endif
