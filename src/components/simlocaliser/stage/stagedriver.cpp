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
#include <stdlib.h>
#include <assert.h>

#include <orcaice/orcaice.h>
#include <orcaplayer/orcaplayer.h>
#include <libplayerc++/playerc++.h>
#include <IceUtil/Thread.h>     // for sleep()

#include "stagedriver.h"

using namespace std;
using namespace orca;
using namespace simlocaliser;
using namespace PlayerCc;

StageDriver::StageDriver( const char *host, int port, const char* id )
    : enabled_( false ),
      robot_(0),
      simulationProxy_(0),
      host_(strdup(host)),
      port_(port),
      id_(strdup(id))
{
}

StageDriver::~StageDriver()
{
}

int
StageDriver::enable()
{
    if ( enabled_ ) return 0;

    cout << "TRACE(playerclientdriver.cpp): StageDriver: Connecting to player on host "
         << host_ << ", port "<<port_ << ", id "<<id_<<endl;
    
    // player throws exceptions on creation if we fail
    try
    {
        robot_      = new PlayerCc::PlayerClient( host_, port_ );
        simulationProxy_ = new PlayerCc::SimulationProxy( robot_, 0 );
    }
    catch ( const PlayerCc::PlayerError & e )
    {
        std::cerr << e << std::endl;
        cout << "ERROR(playerclientdriver.cpp): Error enabling player proxies." << endl;
        disable();
        return -1;
    }

    enabled_ = true;
    return 0;
}

int StageDriver::repair()
{
    disable();
    return enable();
}

int StageDriver::disable()
{
    if ( !enabled_ ) return 0;

    delete simulationProxy_;
    delete robot_;
    enabled_ = false;
    return 0;
}


int
StageDriver::read( orca::Localise2dData& localise2d )
{
    if ( ! enabled_ ) {
        //cout << "ERROR(playerclientdriver.cpp): Can't read: not connected to Player/Stage yet." << endl;
        return -1;
    }

    orcaplayer::convert( *simulationProxy_, localise2d, id_ );
    orcaice::setToNow( localise2d.timeStamp );

    return 0;
}
