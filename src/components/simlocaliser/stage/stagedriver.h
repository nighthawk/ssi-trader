/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#ifndef STAGE_DRIVER_H
#define STAGE_DRIVER_H

#include "../driver.h"

// Player proxies
namespace PlayerCc
{
class PlayerClient;
class SimulationProxy;
}

namespace simlocaliser
{

class StageDriver : public Driver
{
public:

    StageDriver( const char *host, int port, const char* id );
    //StageDriver( const std::map<std::string,std::string> & props );
    virtual ~StageDriver();

    // returns: 0 = success, non-zero = failure
    virtual int enable();
    virtual int repair();
    virtual int disable();

    virtual int read( orca::Localise2dData& position2d );


private:

    bool enabled_;
    PlayerCc::PlayerClient *robot_;
    PlayerCc::SimulationProxy *simulationProxy_;
    
    char *host_;
    int   port_;
    char *id_;
};

} // namespace

#endif
