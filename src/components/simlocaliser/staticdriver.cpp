/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#include <iostream>
#include <assert.h>
#include <IceUtil/Thread.h>     // for sleep()
#include <orcaice/orcaice.h>

#include "staticdriver.h"

using namespace std;
using namespace orca;
using namespace simlocaliser;

StaticDriver::StaticDriver( const orca::Frame2d & pose )
{
    location2d_.hypotheses.resize(1);
    location2d_.hypotheses[0].weight = 1.0;
    
    location2d_.hypotheses[0].mean = pose;

    // should this also be configurable?
    location2d_.hypotheses[0].cov.xx   = 0.01;
    location2d_.hypotheses[0].cov.yy   = 0.01;
    location2d_.hypotheses[0].cov.tt   = 0.01*M_PI/180.0;
    location2d_.hypotheses[0].cov.xy   = 0.0;
    location2d_.hypotheses[0].cov.xt   = 0.0;
    location2d_.hypotheses[0].cov.yt   = 0.0;
}

int
StaticDriver::read( orca::Localise2dData& localise2d )
{
    orcaice::setToNow( location2d_.timeStamp );

    // it's ok just to copy smart pointers because we never modify on our side.
    localise2d = location2d_;

    IceUtil::ThreadControl::sleep(IceUtil::Time::milliSeconds(1000));

    return 0;
}
