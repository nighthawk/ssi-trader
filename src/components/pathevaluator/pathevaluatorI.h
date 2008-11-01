/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */

#ifndef PathEvaluatorI_H
#define PathEvaluatorI_H

#include <IceStorm/IceStorm.h>

#include <map>
#include <string>
#include <gbxsickacfr/gbxiceutilacfr/store.h>
#include <gbxsickacfr/gbxiceutilacfr/buffer.h>

#include <talker/pathevaluator.h>

using namespace std;

namespace pathevaluator
{

class PathEvaluatorI : public talker::PathEvaluator
{
public:
    PathEvaluatorI( gbxiceutilacfr::Buffer<talker::PathEvaluatorTask> &pathEvaluatorTaskStore,
                    const orcaice::Context &context );

    // remote calls
    virtual ::Ice::Int setTask(const talker::PathEvaluatorTask&, const ::Ice::Current& = ::Ice::Current());

    virtual void subscribe(const talker::PathEvaluatorConsumerPrx&, const ::Ice::Current& = ::Ice::Current());
    virtual void unsubscribe(const talker::PathEvaluatorConsumerPrx&, const ::Ice::Current& = ::Ice::Current());

    virtual talker::PathEvaluatorResult getData(const string &sender, const ::Ice::Current& ) const;

    // local calls
    void localSetData(const string &sender, const talker::PathEvaluatorResult &data );
		void cleanStores();


private:
    gbxiceutilacfr::Buffer<talker::PathEvaluatorTask>& pathEvaluatorTaskBuffer_;

    // the driver puts the latest computed path into here using localSetData
		map<string, gbxiceutilacfr::Store<talker::PathEvaluatorResult> * > pathEvaluatorDataStoreMap_;

    // The topic to which we'll publish
    IceStorm::TopicPrx topicPrx_;
    
    // The interface to which we'll publish
    talker::PathEvaluatorConsumerPrx  consumerPrx_;

    orcaice::Context context_;

};

} // namespace

#endif
