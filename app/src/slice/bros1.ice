/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_BROS_1_ICE
#define ORCA2_BROS_1_ICE

// Definitions in this file are based on the discussion in the Player mailing list
// taken place in 2003. Specifically, these are Slice encoding of standard
// definitions proposed by Herman Bruyninckx.

module orca
{

// One dimensional ("linear") objects
// Note: no type aliasing in slice, e.g. typedef (see Ice manual sec.4.21.2)

// double length
// double width
// double height
// double RotationAngle with unlimited range.
// double OrientationAngle with range [-pi,+pi]
// double HeadingAngle with range [-pi/2,+pi/2]
// double AngularVelocity2d (z-dir) with unlimited range
// double AngularAcceleration2d (z-dir) with unlimited range
// double Moment2d (z-dir)


// Two-dimensional ("planar") objects

//! 2D position in Cartesian coordinate system.
struct CartesianPoint2d
{
    //! x-coordinate [m]
    double x;
    //! y-coordinate [m]
    double y;
};
//! 2D velocity in Cartesian coordinate system.
struct CartesianVelocity2d
{
    //! x-coordinate [m/s]
    double x;
    //! y-coordinate [m/s]
    double y;
};
//! 2D acceleration in Cartesian coordinate system.
struct CartesianAcceleration2d
{
    //! x-coordinate [m/s^2]
    double x;
    //! y-coordinate [m/s^2]
    double y;
};
//! 2D position in polar coordinate system.
struct PolarPoint2d
{
    //! Range-coordinate [m]
    double r;
    //! Bearing-coordinate [rad]
    double o;
};
//! 2D velocity in polar coordinate system.
struct PolarVelocity2d
{
    //! Range-coordinate [m/s]
    double r;
    //! Bearing-coordinate [rad/s]
    double o;
};
//! 2D acceleration in polar coordinate system.
struct PolarAcceleration2d
{
    //! Range-coordinate [m/s^2]
    double r;
    //! Bearing-coordinate [rad/s^2]
    double o;
};
//! 2D vector.
struct Vector2d
{
    //! x-coordinate [m]
    double x;
    //! y-coordinate [m]
    double y;
};
//! Planar position.
struct Frame2d
{
    //! Point of origin
    CartesianPoint2d p;
    //! Orientation angle [rad]
    double            o;
};
//! Planar velocity.
struct Twist2d
{
    //! Linear velocity
    CartesianVelocity2d v;
    //! Angular velocity [rad/s]
    double w;
};
//! Vector representing a linear force
struct Force2d
{
    //! x-coordinate [N]
    double x;
    //! y-coordinate [N]
    double y;
};
//! Planar force.
struct Wrench2d
{
    //! Linear force
    Force2d f;
    //! %Moment [N*m]
    double   m;
};
//! Planar continuous dimensions
struct Size2d
{
    //! Length [m]
    double l;
    //! Width [m]
    double w;
};
//! Planar discrete dimensions
struct DiscreteSize2d
{
    //! Length [m]
    int l;
    //! Width [m]
    int w;
};

// Three-dimensional (standard) objects


//! 3D position in Cartesian coordinate system.
struct CartesianPoint
{
    //! x-coordinate [m]
    double x;
    //! y-coordinate [m]
    double y;
    //! z-coordinate [m]
    double z;
};
//! 3D vector
struct CartesianVector
{
    //! x-coordinate [m]
    double x;
    //! y-coordinate [m]
    double y;
    //! z-coordinate [m]
    double z;
};
//! 3D velocity in Cartesian coordinate system.
struct CartesianVelocity
{
    //! x-coordinate [m/s]
    double x;
    //! y-coordinate [m/s]
    double y;
    //! z-coordinate [m/s]
    double z;
};
//! 3D acceleration in Cartesian coordinate system.
struct CartesianAcceleration
{
    //! x-coordinate [m/s^2]
    double x;
    //! y-coordinate [m/s^2]
    double y;
    //! z-coordinate [m/s^2]
    double z;
};
//! 3D angular velocity.
struct AngularVelocity
{
    //! Angular velocity around the x-axis [rad/s]
    double x;
    //! Angular velocity around the y-axis [rad/s]
    double y;
    //! Angular velocity around the z-axis [rad/s]
    double z;
};
//! 3D angular acceleration.
struct AngularAcceleration
{
    //! Angular acceleration around the x-axis [rad/s]
    double x;
    //! Angular acceleration around the y-axis [rad/s]
    double y;
    //! Angular acceleration around the z-axis [rad/s]
    double z;
};
//! Vector representing a linear force
struct Force
{
    //! x-coordinate [N]
    double x;
    //! y-coordinate [N]
    double y;
    //! z-coordinate [N]
    double z;
};
//! %Moment of force.
struct Moment
{
    //! x-coordinate [N*m]
    double x;
    //! y-coordinate [N*m]
    double y;
    //! z-coordinate [N*m]
    double z;
};
//! Spatial velocity: ordered couple (V,W) of two vectors
struct Twist3d
{
    //! Linear velocity
    CartesianVelocity v;
    //! Angular velocity
    AngularVelocity w;
};
//! Spatial force: ordered couple (F,M) of two vectors
struct Wrench
{
    //! Linear force
    Force  f;
    //! %Moment of force
    Moment m;
};
//! Orientation defined by angles
struct OrientationE
{
    //! Roll angle [rad]
    double r;
    //! Pitch angle [rad]
    double p;
    //! Yaw angle [rad]
    double y;
};
//! Spatial position
struct Frame3d
{
    //! Planar position
    CartesianPoint p;
    //! Orientation
    OrientationE   o;
};
//! Spatial continuous dimensions.
struct Size3d
{
    //! Length [m]
    double l;
    //! Width [m]
    double w;
    //! Height [m]
    double h;
};
//! Spatial discrete dimensions.
struct DiscreteSize3d
{
    //! Length [m]
    int l;
    //! Width [m]
    int w;
    //! Height [m]
    int h;
};

}; // module

#endif
