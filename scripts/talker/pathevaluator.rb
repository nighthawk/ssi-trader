# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `pathevaluator.ice'

require 'Ice'
require 'orca/common.rb'
require 'orca/bros1.rb'
require 'orca/pathfollower2d.rb'

module Talker

    if not defined?(::Talker::Task2d)
        class Task2d
            def initialize(target=::Orca::Frame2d.new)
                @target = target
            end

            def hash
                _h = 0
                _h = 5 * _h + @target.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @target != other.target
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Task2d)
            end

            attr_accessor :target
        end

        T_Task2d = ::Ice::__defineStruct('::talker::Task2d', Task2d, [["target", ::Orca::T_Frame2d]])
    end

    if not defined?(::Talker::T_TaskList2d)
        T_TaskList2d = ::Ice::__defineSequence('::talker::TaskList2d', ::Talker::T_Task2d)
    end

    if not defined?(::Talker::Bundle2d)
        class Bundle2d
            def initialize(cost=0.0, tasks=nil)
                @cost = cost
                @tasks = tasks
            end

            def hash
                _h = 0
                _h = 5 * _h + @cost.hash
                _h = 5 * _h + @tasks.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @cost != other.cost or
                    @tasks != other.tasks
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Bundle2d)
            end

            attr_accessor :cost, :tasks
        end

        T_Bundle2d = ::Ice::__defineStruct('::talker::Bundle2d', Bundle2d, [
            ["cost", ::Ice::T_float],
            ["tasks", ::Talker::T_TaskList2d]
        ])
    end

    if not defined?(::Talker::T_BundleList2d)
        T_BundleList2d = ::Ice::__defineSequence('::talker::BundleList2d', ::Talker::T_Bundle2d)
    end

    if not defined?(::Talker::PathEvaluatorConsumer_mixin)
        module PathEvaluatorConsumer_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::talker::PathEvaluatorConsumer']
            end

            def ice_id(current=nil)
                '::talker::PathEvaluatorConsumer'
            end

            #
            # Operation signatures.
            #
            # def setData(obj, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathEvaluatorConsumer)
            end
        end
        class PathEvaluatorConsumer
            include PathEvaluatorConsumer_mixin
        end
        module PathEvaluatorConsumerPrx_mixin

            def setData(obj, _ctx=nil)
                PathEvaluatorConsumer_mixin::OP_setData.invoke(self, [obj], _ctx)
            end
        end
        class PathEvaluatorConsumerPrx < ::Ice::ObjectPrx
            include PathEvaluatorConsumerPrx_mixin

            def PathEvaluatorConsumerPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::talker::PathEvaluatorConsumer', facetOrCtx, _ctx)
            end

            def PathEvaluatorConsumerPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Talker::T_PathEvaluatorConsumer)
            T_PathEvaluatorConsumer = ::Ice::__declareClass('::talker::PathEvaluatorConsumer')
            T_PathEvaluatorConsumerPrx = ::Ice::__declareProxy('::talker::PathEvaluatorConsumer')
        end

        T_PathEvaluatorConsumer.defineClass(PathEvaluatorConsumer, true, nil, [], [])
        PathEvaluatorConsumer_mixin::ICE_TYPE = T_PathEvaluatorConsumer

        T_PathEvaluatorConsumerPrx.defineProxy(PathEvaluatorConsumerPrx, T_PathEvaluatorConsumer)
        PathEvaluatorConsumerPrx::ICE_TYPE = T_PathEvaluatorConsumerPrx

        PathEvaluatorConsumer_mixin::OP_setData = ::Ice::__defineOperation('setData', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_BundleList2d], [], nil, [])
    end

    if not defined?(::Talker::PathEvaluatorTask)
        class PathEvaluatorTask
            def initialize(maxBundles=0, bundleSize=0, newTasks=nil, committedTasks=nil, start=::Orca::Frame2d.new, prx=nil)
                @maxBundles = maxBundles
                @bundleSize = bundleSize
                @newTasks = newTasks
                @committedTasks = committedTasks
                @start = start
                @prx = prx
            end

            def hash
                _h = 0
                _h = 5 * _h + @maxBundles.hash
                _h = 5 * _h + @bundleSize.hash
                _h = 5 * _h + @newTasks.hash
                _h = 5 * _h + @committedTasks.hash
                _h = 5 * _h + @start.hash
                _h = 5 * _h + @prx.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @maxBundles != other.maxBundles or
                    @bundleSize != other.bundleSize or
                    @newTasks != other.newTasks or
                    @committedTasks != other.committedTasks or
                    @start != other.start or
                    @prx != other.prx
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PathEvaluatorTask)
            end

            attr_accessor :maxBundles, :bundleSize, :newTasks, :committedTasks, :start, :prx
        end

        T_PathEvaluatorTask = ::Ice::__defineStruct('::talker::PathEvaluatorTask', PathEvaluatorTask, [
            ["maxBundles", ::Ice::T_int],
            ["bundleSize", ::Ice::T_int],
            ["newTasks", ::Talker::T_TaskList2d],
            ["committedTasks", ::Talker::T_TaskList2d],
            ["start", ::Orca::T_Frame2d],
            ["prx", ::Talker::T_PathEvaluatorConsumerPrx]
        ])
    end

    if not defined?(::Talker::PathEvaluator_mixin)
        module PathEvaluator_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::talker::PathEvaluator']
            end

            def ice_id(current=nil)
                '::talker::PathEvaluator'
            end

            #
            # Operation signatures.
            #
            # def setTask(task, current=nil)
            # def getData(current=nil)
            # def subscribe(subscriber, current=nil)
            # def unsubscribe(subscriber, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathEvaluator)
            end
        end
        class PathEvaluator
            include PathEvaluator_mixin
        end
        module PathEvaluatorPrx_mixin

            def setTask(task, _ctx=nil)
                PathEvaluator_mixin::OP_setTask.invoke(self, [task], _ctx)
            end

            def getData(_ctx=nil)
                PathEvaluator_mixin::OP_getData.invoke(self, [], _ctx)
            end

            def subscribe(subscriber, _ctx=nil)
                PathEvaluator_mixin::OP_subscribe.invoke(self, [subscriber], _ctx)
            end

            def unsubscribe(subscriber, _ctx=nil)
                PathEvaluator_mixin::OP_unsubscribe.invoke(self, [subscriber], _ctx)
            end
        end
        class PathEvaluatorPrx < ::Ice::ObjectPrx
            include PathEvaluatorPrx_mixin

            def PathEvaluatorPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::talker::PathEvaluator', facetOrCtx, _ctx)
            end

            def PathEvaluatorPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Talker::T_PathEvaluator)
            T_PathEvaluator = ::Ice::__declareClass('::talker::PathEvaluator')
            T_PathEvaluatorPrx = ::Ice::__declareProxy('::talker::PathEvaluator')
        end

        T_PathEvaluator.defineClass(PathEvaluator, true, nil, [], [])
        PathEvaluator_mixin::ICE_TYPE = T_PathEvaluator

        T_PathEvaluatorPrx.defineProxy(PathEvaluatorPrx, T_PathEvaluator)
        PathEvaluatorPrx::ICE_TYPE = T_PathEvaluatorPrx

        PathEvaluator_mixin::OP_setTask = ::Ice::__defineOperation('setTask', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_PathEvaluatorTask], [], ::Ice::T_int, [::Orca::T_BusyException, ::Orca::T_RequiredInterfaceFailedException])
        PathEvaluator_mixin::OP_getData = ::Ice::__defineOperation('getData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], ::Talker::T_BundleList2d, [])
        PathEvaluator_mixin::OP_subscribe = ::Ice::__defineOperation('subscribe', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_PathEvaluatorConsumerPrx], [], nil, [::Orca::T_SubscriptionFailedException])
        PathEvaluator_mixin::OP_unsubscribe = ::Ice::__defineOperation('unsubscribe', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Talker::T_PathEvaluatorConsumerPrx], [], nil, [])
    end
end
