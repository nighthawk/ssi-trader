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
PathEvaluatorI::getData(const string& sender, const Ice::Current& current ) const  {
    // we don't need to pop the data here because we don't block on it.

		gbxiceutilacfr::Store<PathEvaluatorResult> * dataStore = NULL;
		map<string, gbxiceutilacfr::Store<talker::PathEvaluatorResult> * >::const_iterator iter;
		for (iter = pathEvaluatorDataStoreMap_.begin(); iter != pathEvaluatorDataStoreMap_.end(); ++iter) {
			if (iter->first == sender) {
				dataStore = iter->second;
			}
		}
		
    PathEvaluatorResult data;
		if (dataStore != NULL) {
	    if ( dataStore->isEmpty() )
				throw orca::DataNotExistException( "try again later." );
			else
	    	dataStore->get( data );
		}

    return data;
}

void 
PathEvaluatorI::localSetData(const string& sender, const PathEvaluatorResult& data ) {
		gbxiceutilacfr::Store<PathEvaluatorResult> * dataStore = NULL;
		map<string, gbxiceutilacfr::Store<talker::PathEvaluatorResult> * >::iterator iter;
		for (iter = pathEvaluatorDataStoreMap_.begin(); iter != pathEvaluatorDataStoreMap_.end(); ++iter) {
			if (iter->first == sender) {
				dataStore = iter->second;

	      stringstream ssPath;
	      ssPath << "PathEvaluatorI::localSetData: Re-using data store for " << sender << endl;
	      context_.tracer().debug( ssPath.str() );
	
				break;
			}
		}
		
		if (dataStore == NULL) {
      stringstream ssPath;
      ssPath << "PathEvaluatorI::localSetData: Creating data store for " << sender << endl;
      context_.tracer().debug( ssPath.str() );

			dataStore = new gbxiceutilacfr::Store<PathEvaluatorResult>();

			pathEvaluatorDataStoreMap_.insert(make_pair(sender, dataStore));
		}

		dataStore->set(data);

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
PathEvaluatorI::cleanStores() {
	int i = 0;
	
	map<string, gbxiceutilacfr::Store<talker::PathEvaluatorResult> * >::iterator iter;
	for (iter = pathEvaluatorDataStoreMap_.begin(); iter != pathEvaluatorDataStoreMap_.end(); ++iter) {
		if (! iter->second->isNewData()) {
			pathEvaluatorDataStoreMap_.erase(iter);
			i++;
		}
	}
	
  stringstream ssPath;
  ssPath << "PathEvaluatorI::cleanStores: " << i << " stores flushed." << endl;
  context_.tracer().info( ssPath.str() );
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
