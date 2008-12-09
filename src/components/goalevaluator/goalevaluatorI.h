/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#ifndef GoalEvaluatorI_H
#define GoalEvaluatorI_H

#include <IceStorm/IceStorm.h>

#include <map>
#include <string>
#include <gbxsickacfr/gbxiceutilacfr/store.h>
#include <gbxsickacfr/gbxiceutilacfr/buffer.h>

#include <talker/goalevaluator.h>

using namespace std;

namespace goalevaluator
{

class GoalEvaluatorI : public talker::GoalEvaluator
{
public:
    GoalEvaluatorI( gbxiceutilacfr::Buffer<talker::GoalEvaluatorTask> &goalEvaluatorTaskStore,
                    const orcaice::Context &context );

    // remote calls
    virtual ::Ice::Int setTask(const talker::GoalEvaluatorTask&, const ::Ice::Current& = ::Ice::Current());

    virtual void subscribe(const talker::GoalEvaluatorConsumerPrx&, const ::Ice::Current& = ::Ice::Current());
    virtual void unsubscribe(const talker::GoalEvaluatorConsumerPrx&, const ::Ice::Current& = ::Ice::Current());

    virtual talker::GoalEvaluatorResult getData(const string &sender, const ::Ice::Current& ) const;

    // local calls
    void localSetData(const string &sender, const talker::GoalEvaluatorResult &data );
		void cleanStores();


private:
    gbxiceutilacfr::Buffer<talker::GoalEvaluatorTask>& goalEvaluatorTaskBuffer_;

    // the driver puts the latest computed path into here using localSetData
		map<string, gbxiceutilacfr::Store<talker::GoalEvaluatorResult> * > goalEvaluatorDataStoreMap_;

    // The topic to which we'll publish
    IceStorm::TopicPrx topicPrx_;
    
    // The interface to which we'll publish
    talker::GoalEvaluatorConsumerPrx  consumerPrx_;

    orcaice::Context context_;

};

} // namespace

#endif
