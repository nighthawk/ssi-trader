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
#include <orcaice/orcaice.h>
#include <orcaobj/orcaobj.h>
#include <orcaifaceimpl/localise2dImpl.h>

#include "mainthread.h"
#include "staticdriver.h"
#ifdef HAVE_STAGE_DRIVER
#   include "stage/stagedriver.h"
#endif

using namespace std;
using namespace simlocaliser;

MainThread::MainThread( const orcaice::Context & context ) :  
    SafeThread(context.tracer()),
    context_(context)
{
    //
    // Read settings
    //
    string prefix = context_.tag() + ".Config.";

		update_interval = orcaice::getPropertyAsDoubleWithDefault( context_.properties(), prefix+"UpdateInterval", 0.1 );

    // based on the config parameter, create the right driver
    string driverName = orcaice::getPropertyWithDefault( context_.properties(),
            prefix+"Driver", "static" );

            
    if ( driverName == "static" )
    {      
        std::string driverPrefix = prefix + "Static.";
        orca::Frame2d pose;
        orcaobj::setInit(pose);
        pose = orcaobj::getPropertyAsFrame2dWithDefault( context_.properties(),
                driverPrefix+"Pose", pose );
                
        driver_ = new StaticDriver( pose );
    }
    else if ( driverName == "stage" )
    {
#ifdef HAVE_STAGE_DRIVER
        context_.tracer().debug( "loading Player-Client driver",3);
        
        std::string driverPrefix = prefix + "Stage.";
        std::string playerHost = orcaice::getPropertyWithDefault( context_.properties(),
                driverPrefix+"Host", "localhost" );
        int playerPort = orcaice::getPropertyAsIntWithDefault( context_.properties(),
                driverPrefix+"Port", 6665 );
        std::string playerId = orcaice::getPropertyWithDefault( context_.properties(),
                driverPrefix+"Id", "model1" );
    
        driver_ = new StageDriver( playerHost.c_str(), playerPort, playerId.c_str() );
#else
        // unrecoverable error
        context_.shutdown();
        throw gbxutilacfr::Exception( ERROR_INFO, "Can't instantiate driver 'stage' because it was not built!" );
#endif
    }
    
    context_.tracer().debug("driver instantiated",5);
}

MainThread::~MainThread()
{
}

void
MainThread::walk()
{
    // we are in a different thread now, catch all stray exceptions
    try
    {

    //
    // EXTERNAL PROVIDED INTERFACE
    //
    
    orca::VehicleGeometryCuboidDescriptionPtr geom = new orca::VehicleGeometryCuboidDescription;
    geom->type = orca::VehicleGeometryCuboid;
    orcaobj::setSane( geom->size );
    orcaobj::setSane( geom->vehicleToGeometryTransform );
    
    orcaifaceimpl::Localise2dImplPtr iface;
    iface = new orcaifaceimpl::Localise2dImpl( geom, "Localise2d", context_ );
    iface->initInterface( this );

    //
    // ENABLE NETWORK CONNECTIONS
    //
    // this may throw, but may as well quit right then
    orcaice::activate( context_, this );

    //
    // Enable driver
    //
    while ( !isStopping() && driver_->enable() ) {
        context_.tracer().warning("failed to enable the driver; will try again in 2 seconds.");
        IceUtil::ThreadControl::sleep(IceUtil::Time::seconds(2));
    }
    context_.tracer().info("driver enabled");

    // This is the main loop
    try 
    {
        int readStatus;
         
        while ( !isStopping() )
        {

            //
            // Read data
            //
            orca::Localise2dData localiseData;
            // readTimer_.restart();
            readStatus = driver_->read( localiseData );
            // cout<<"read: " << readTimer_.elapsed().toMilliSecondsDouble()<<endl;
        
            if ( readStatus==0 ) 
            {
                iface->localSetAndSend( localiseData );
            } 
            else 
            {
                context_.tracer().error("failed to read data from Segway hardware. Repairing....");
                driver_->repair();
            }

						// wait for update interval
						IceUtil::ThreadControl::sleep(IceUtil::Time::seconds(update_interval));
        }
    }
    catch ( Ice::CommunicatorDestroyedException &e )
    {
        // This is OK: it means that the communicator shut down (eg via Ctrl-C)
        // somewhere in mainLoop.
        //
        // Could probably handle it better for an Application by stopping the component on Ctrl-C
        // before shutting down communicator.
    }

    // reset the hardware
    if ( driver_->disable() ) {
        context_.tracer().warning("failed to disable driver");
    }
    else {
        context_.tracer().debug("driver disabled",5);
    }

    //
    // unexpected exceptions
    //
    } // try
    catch ( const orca::OrcaException & e )
    {
        stringstream ss;
        ss << "unexpected (remote?) orca exception: " << e << ": " << e.what;
        context_.tracer().error( ss.str() );
        if ( context_.isApplication() ) {
            context_.tracer().info( "this is an stand-alone component. Quitting...");
            context_.communicator()->destroy();
        }
    }
    catch ( const gbxutilacfr::Exception & e )
    {
        stringstream ss;
        ss << "unexpected (local?) orcaice exception: " << e.what();
        context_.tracer().error( ss.str() );
        if ( context_.isApplication() ) {
            context_.tracer().info( "this is an stand-alone component. Quitting...");
            context_.communicator()->destroy();
        }
    }
    catch ( const Ice::Exception & e )
    {
        stringstream ss;
        ss << "unexpected Ice exception: " << e;
        context_.tracer().error( ss.str() );
        if ( context_.isApplication() ) {
            context_.tracer().info( "this is an stand-alone component. Quitting...");
            context_.communicator()->destroy();
        }
    }
    catch ( const std::exception & e )
    {
        // once caught this beast in here, don't know who threw it 'St9bad_alloc'
        stringstream ss;
        ss << "unexpected std exception: " << e.what();
        context_.tracer().error( ss.str() );
        if ( context_.isApplication() ) {
            context_.tracer().info( "this is an stand-alone component. Quitting...");
            context_.communicator()->destroy();
        }
    }
    catch ( ... )
    {
        context_.tracer().error( "unexpected exception from somewhere.");
        if ( context_.isApplication() ) {
            context_.tracer().info( "this is an stand-alone component. Quitting...");
            context_.communicator()->destroy();
        }
    }
    
    // wait for the component to realize that we are quitting and tell us to stop.
    waitForStop();
}
