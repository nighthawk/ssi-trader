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

#include "goalevaluatorI.h"

using namespace std;
using namespace talker;
using namespace goalevaluator;

GoalEvaluatorI::GoalEvaluatorI( gbxiceutilacfr::Buffer<talker::GoalEvaluatorTask> &goalEvaluatorTaskBuffer,
                                const orcaice::Context &context )
            : goalEvaluatorTaskBuffer_(goalEvaluatorTaskBuffer),
              context_(context) 
{
    // Find IceStorm Topic to which we'll publish
    topicPrx_ = orcaice::connectToTopicWithTag<GoalEvaluatorConsumerPrx> ( context_, consumerPrx_, "GoalEvaluator" );    
}

Ice::Int
GoalEvaluatorI::setTask(const GoalEvaluatorTask& task, const ::Ice::Current&) {
    context_.tracer().debug( "GoalEvaluatorI::setTask: Received new task." );

    int numAhead = goalEvaluatorTaskBuffer_.size();
    goalEvaluatorTaskBuffer_.push( task );

    return numAhead;
}

talker::GoalEvaluatorResult
GoalEvaluatorI::getData(const string& sender, const Ice::Current& current ) const  {
    // we don't need to pop the data here because we don't block on it.

		gbxiceutilacfr::Store<GoalEvaluatorResult> * dataStore = NULL;
		map<string, gbxiceutilacfr::Store<talker::GoalEvaluatorResult> * >::const_iterator iter;
		for (iter = goalEvaluatorDataStoreMap_.begin(); iter != goalEvaluatorDataStoreMap_.end(); ++iter) {
			if (iter->first == sender) {
				dataStore = iter->second;
			}
		}
		
    GoalEvaluatorResult data;
		if (dataStore != NULL) {
	    if ( dataStore->isEmpty() )
				throw orca::DataNotExistException( "try again later." );
			else
	    	dataStore->get( data );
		}

    return data;
}

void 
GoalEvaluatorI::localSetData(const string& sender, const GoalEvaluatorResult& data ) {
		gbxiceutilacfr::Store<GoalEvaluatorResult> * dataStore = NULL;
		map<string, gbxiceutilacfr::Store<talker::GoalEvaluatorResult> * >::iterator iter;
		for (iter = goalEvaluatorDataStoreMap_.begin(); iter != goalEvaluatorDataStoreMap_.end(); ++iter) {
			if (iter->first == sender) {
				dataStore = iter->second;

	      stringstream ssPath;
	      ssPath << "GoalEvaluatorI::localSetData: Re-using data store for " << sender << endl;
	      context_.tracer().debug( ssPath.str() );
	
				break;
			}
		}
		
		if (dataStore == NULL) {
      stringstream ssPath;
      ssPath << "GoalEvaluatorI::localSetData: Creating data store for " << sender << endl;
      context_.tracer().debug( ssPath.str() );

			dataStore = new gbxiceutilacfr::Store<GoalEvaluatorResult>();

			goalEvaluatorDataStoreMap_.insert(make_pair(sender, dataStore));
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
        context_.tracer().warning( "GoalEvaluatorI::localSetData: Failed push to IceStorm." );
    }
}

void
GoalEvaluatorI::cleanStores() {
	int i = 0;
	
	map<string, gbxiceutilacfr::Store<talker::GoalEvaluatorResult> * >::iterator iter;
	for (iter = goalEvaluatorDataStoreMap_.begin(); iter != goalEvaluatorDataStoreMap_.end(); ++iter) {
		if (! iter->second->isNewData()) {
			goalEvaluatorDataStoreMap_.erase(iter);
			i++;
		}
	}
	
  stringstream ssPath;
  ssPath << "GoalEvaluatorI::cleanStores: " << i << " stores flushed." << endl;
  context_.tracer().info( ssPath.str() );
}

void 
GoalEvaluatorI::subscribe(const GoalEvaluatorConsumerPrx &subscriber, const ::Ice::Current&) {
    try {
        topicPrx_->subscribeAndGetPublisher( IceStorm::QoS(), subscriber->ice_twoway() );
    }
    catch ( const IceStorm::AlreadySubscribed & e ) {
        std::stringstream ss;
        ss <<"GoalEvaluatorI::subscribe: Request for subscribe but this proxy has already been subscribed, so I do nothing: "<< e;
        context_.tracer().debug( ss.str(), 2 );    
    }
    catch ( const Ice::Exception & e ) {
        std::stringstream ss;
        ss <<"GoalEvaluatorI::subscribe: failed to subscribe: "<< e << endl;
        context_.tracer().warning( ss.str() );
        throw orca::SubscriptionFailedException( ss.str() );
    }
}

void 
GoalEvaluatorI::unsubscribe(const GoalEvaluatorConsumerPrx &subscriber, const ::Ice::Current&) {
    topicPrx_->unsubscribe( subscriber );
}
