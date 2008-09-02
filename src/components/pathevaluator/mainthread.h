/*
 * Orca-Robotics Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in
 * the LICENSE file included in this distribution.
 *
 */
 
#ifndef MAINTHREAD_H
#define MAINTHREAD_H

#include <orcaice/subsystemthread.h>
#include <orcaice/context.h>

#include <gbxsickacfr/gbxiceutilacfr/buffer.h>
#include <hydronavutil/pose.h>
#include <orca/pathplanner2d.h>
#include <memory>
#include <orcaifaceimpl/storingconsumers.h>

#include <talker/pathevaluator.h>
#include "pathevaluatorI.h"

namespace pathevaluator
{

class MainThread : public orcaice::SubsystemThread
{

public: 

	MainThread( const orcaice::Context& context );

	virtual void walk();


private:
	void initNetwork();

	talker::BundleList2d createBundles( talker::PathEvaluatorTask task );

	float computePathCost( talker::Task2d start, talker::TaskList2d &tasks );

	// filters tasks by removing "helpless" ones, i.e. those that
	// are unlikely to result in a competitive bundle
	void filterTasks( talker::Task2d &cur, talker::TaskList2d &tasks,
										unsigned int bundleSize, unsigned int maxBundles );

	// calculates a total of $maxBundles which include at max
	// $bundleSize of new $tasks, always starting with $start and
	// always including all of $committed
	talker::BundleList2d findBundles( talker::Task2d &start,
																		talker::TaskList2d &committed,
																		talker::TaskList2d &tasks,
																		unsigned int bundleSize,
																		unsigned int maxBundles );

	// used for provided interface
	PathEvaluatorI* pathEvaluatorI_;
	gbxiceutilacfr::Buffer<talker::PathEvaluatorTask> pathEvaluatorTaskBuffer_;

	// required interface to pathplanner
	orca::PathPlanner2dPrx pathplanner2dPrx_;

	// receives and stores information about computed paths 
	orcaifaceimpl::StoringPathPlanner2dConsumerImplPtr computedPathConsumer_;

	// If the path planner takes more than this amount of time, assume something's wrong.
	double pathPlanTimeout_;

	orcaice::Context context_;

};

} // namespace

#endif
