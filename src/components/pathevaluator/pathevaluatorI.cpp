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

#include "pathevaluatorI.h"

using namespace std;
using namespace talker;
using namespace pathevaluator;

PathEvaluatorI::PathEvaluatorI( gbxiceutilacfr::Buffer<talker::PathEvaluatorTask> &pathEvaluatorTaskBuffer,
                                const orcaice::Context &context )
            : pathEvaluatorTaskBuffer_(pathEvaluatorTaskBuffer),
              context_(context) 
{
    // Find IceStorm Topic to which we'll publish
    topicPrx_ = orcaice::connectToTopicWithTag<PathEvaluatorConsumerPrx> ( context_, consumerPrx_, "PathEvaluator" );    
}

Ice::Int
PathEvaluatorI::setTask(const PathEvaluatorTask& task, const ::Ice::Current&) {
    context_.tracer().debug( "PathEvaluatorI::setTask: Received new task." );

    int numAhead = pathEvaluatorTaskBuffer_.size();
    pathEvaluatorTaskBuffer_.push( task );

    return numAhead;
}

talker::PathEvaluatorResult
PathEvaluatorI::getData(const Ice::Current& current ) const  {
    // we don't need to pop the data here because we don't block on it.
    if ( pathEvaluatorDataStore_.isEmpty() )
        throw orca::DataNotExistException( "try again later." );

    PathEvaluatorResult data;
    pathEvaluatorDataStore_.get( data );

    return data;
}

void 
PathEvaluatorI::localSetData( const PathEvaluatorResult& data ) {
    pathEvaluatorDataStore_.set( data );

    // Try to push it out to IceStorm too
    try {
        consumerPrx_->setData( data );
    }
    catch ( Ice::ConnectionRefusedException &e )
    {
        // This could happen if IceStorm dies.
        // If we're running in an IceBox and the IceBox is shutting down, 
        // this is expected (our co-located IceStorm is obviously going down).
        context_.tracer().warning( "PathEvaluatorI::localSetData: Failed push to IceStorm." );
    }
}

void 
PathEvaluatorI::subscribe(const PathEvaluatorConsumerPrx &subscriber, const ::Ice::Current&) {
    try {
        topicPrx_->subscribeAndGetPublisher( IceStorm::QoS(), subscriber->ice_twoway() );
    }
    catch ( const IceStorm::AlreadySubscribed & e ) {
        std::stringstream ss;
        ss <<"PathEvaluatorI::subscribe: Request for subscribe but this proxy has already been subscribed, so I do nothing: "<< e;
        context_.tracer().debug( ss.str(), 2 );    
    }
    catch ( const Ice::Exception & e ) {
        std::stringstream ss;
        ss <<"PathEvaluatorI::subscribe: failed to subscribe: "<< e << endl;
        context_.tracer().warning( ss.str() );
        throw orca::SubscriptionFailedException( ss.str() );
    }
}

void 
PathEvaluatorI::unsubscribe(const PathEvaluatorConsumerPrx &subscriber, const ::Ice::Current&) {
    topicPrx_->unsubscribe( subscriber );
}
