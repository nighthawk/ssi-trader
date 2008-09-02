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

#include <gbxsickacfr/gbxiceutilacfr/store.h>
#include <gbxsickacfr/gbxiceutilacfr/buffer.h>

#include <talker/pathevaluator.h>

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

    virtual talker::BundleList2d getData(const ::Ice::Current& ) const;

    // local calls
    void localSetData( const talker::BundleList2d &data );

private:
    gbxiceutilacfr::Buffer<talker::PathEvaluatorTask>& pathEvaluatorTaskBuffer_;

    // the driver puts the latest computed path into here using localSetData
    gbxiceutilacfr::Store<talker::BundleList2d> pathEvaluatorDataStore_;

    // The topic to which we'll publish
    IceStorm::TopicPrx topicPrx_;
    
    // The interface to which we'll publish
    talker::PathEvaluatorConsumerPrx  consumerPrx_;

    orcaice::Context context_;

};

} // namespace

#endif
