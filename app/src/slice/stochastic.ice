/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_STOCHASTIC_ICE
#define ORCA2_STOCHASTIC_ICE

// Definitions in this file are ment to be building blocks used by interface definitions.

module orca
{

//! 2d Covariance Matrix
//! @see Frame2d
struct Covariance2d
{
    //! Matrix index (0,0)
    double xx;
    //! Matrix index (0,1)
    double xy;
    //! Matrix index (1,1)
    double yy;
    //! Matrix index (0,2)
    double xt;
    //! Matrix index (1,2)
    double yt;
    //! Matrix index (2,2)
    double tt;
};

//! 3d Covariance Matrix (x,y,z,roll,pitch,azimuth)
//! @see Frame3d
struct Covariance3d
{
    //! Matrix index (0,0)
    double xx;
    //! Matrix index (0,1)
    double xy;
    //! Matrix index (0,2)
    double xz;
    //! Matrix index (0,3)
    double xr;
    //! Matrix index (0,4)
    double xp;
    //! Matrix index (0,5)
    double xa;
        
    //! Matrix index (1,1)
    double yy;
    //! Matrix index (1,2)
    double yz;
    //! Matrix index (1,3)
    double yr;
    //! Matrix index (1,4)
    double yp;
    //! Matrix index (1,5)
    double ya;
     
    //! Matrix index (2,2)
    double zz;
    //! Matrix index (2,3)
    double zr;
    //! Matrix index (2,4)
    double zp;
    //! Matrix index (2,5)
    double za;

    //! Matrix index (3,3)
    double rr;
    //! Matrix index (3,4)
    double rp;
    //! Matrix index (3,5)
    double ra;

    //! Matrix index (4,4)
    double pp;
    //! Matrix index (4,5)
    double pa;
    
    //! Matrix index (5,5)
    double aa;
};


}; // module

#endif
