# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `goalevaluator.ice'

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

    if not defined?(::Talker::GoalEvaluatorResult)
        class GoalEvaluatorResult
            def initialize(id='', data=nil)
                @id = id
                @data = data
            end

            def hash
                _h = 0
                _h = 5 * _h + @id.hash
                _h = 5 * _h + @data.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @id != other.id or
                    @data != other.data
                true
            end

            def inspect
                ::Ice::__stringify(self, T_GoalEvaluatorResult)
            end

            attr_accessor :id, :data
        end

        T_GoalEvaluatorResult = ::Ice::__defineStruct('::talker::GoalEvaluatorResult', GoalEvaluatorResult, [
            ["id", ::Ice::T_string],
            ["data", ::Talker::T_BundleList2d]
        ])
    end

    if not defined?(::Talker::GoalEvaluatorConsumer_mixin)
        module GoalEvaluatorConsumer_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::talker::GoalEvaluatorConsumer']
            end

            def ice_id(current=nil)
                '::talker::GoalEvaluatorConsumer'
            end

            #
            # Operation signatures.
            #
            # def setData(obj, current=nil)

            def inspect
                ::Ice::__stringify(self, T_GoalEvaluatorConsumer)
            end
        end
        class GoalEvaluatorConsumer
            include GoalEvaluatorConsumer_mixin
        end
        module GoalEvaluatorConsumerPrx_mixin

            def setData(obj, _ctx=nil)
                GoalEvaluatorConsumer_mixin::OP_setData.invoke(self, [obj], _ctx)
            end
        end
        class GoalEvaluatorConsumerPrx < ::Ice::ObjectPrx
            include GoalEvaluatorConsumerPrx_mixin

            def GoalEvaluatorConsumerPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::talker::GoalEvaluatorConsumer', facetOrCtx, _ctx)
            end

            def GoalEvaluatorConsumerPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Talker::T_GoalEvaluatorConsumer)
            T_GoalEvaluatorConsumer = ::Ice::__declareClass('::talker::GoalEvaluatorConsumer')
            T_GoalEvaluatorConsumerPrx = ::Ice::__declareProxy('::talker::GoalEvaluatorConsumer')
        end

        T_GoalEvaluatorConsumer.defineClass(GoalEvaluatorConsumer, true, nil, [], [])
        GoalEvaluatorConsumer_mixin::ICE_TYPE = T_GoalEvaluatorConsumer

        T_GoalEvaluatorConsumerPrx.defineProxy(GoalEvaluatorConsumerPrx, T_GoalEvaluatorConsumer)
        GoalEvaluatorConsumerPrx::ICE_TYPE = T_GoalEvaluatorConsumerPrx

        GoalEvaluatorConsumer_mixin::OP_setData = ::Ice::__defineOperation('setData', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_GoalEvaluatorResult], [], nil, [])
    end

    if not defined?(::Talker::GoalEvaluatorTask)
        class GoalEvaluatorTask
            def initialize(id='', sender='', maxBundles=0, bundleSize=0, newTasks=nil, committedTasks=nil, start=::Orca::Frame2d.new, prx=nil)
                @id = id
                @sender = sender
                @maxBundles = maxBundles
                @bundleSize = bundleSize
                @newTasks = newTasks
                @committedTasks = committedTasks
                @start = start
                @prx = prx
            end

            def hash
                _h = 0
                _h = 5 * _h + @id.hash
                _h = 5 * _h + @sender.hash
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
                    @id != other.id or
                    @sender != other.sender or
                    @maxBundles != other.maxBundles or
                    @bundleSize != other.bundleSize or
                    @newTasks != other.newTasks or
                    @committedTasks != other.committedTasks or
                    @start != other.start or
                    @prx != other.prx
                true
            end

            def inspect
                ::Ice::__stringify(self, T_GoalEvaluatorTask)
            end

            attr_accessor :id, :sender, :maxBundles, :bundleSize, :newTasks, :committedTasks, :start, :prx
        end

        T_GoalEvaluatorTask = ::Ice::__defineStruct('::talker::GoalEvaluatorTask', GoalEvaluatorTask, [
            ["id", ::Ice::T_string],
            ["sender", ::Ice::T_string],
            ["maxBundles", ::Ice::T_int],
            ["bundleSize", ::Ice::T_int],
            ["newTasks", ::Talker::T_TaskList2d],
            ["committedTasks", ::Talker::T_TaskList2d],
            ["start", ::Orca::T_Frame2d],
            ["prx", ::Talker::T_GoalEvaluatorConsumerPrx]
        ])
    end

    if not defined?(::Talker::GoalEvaluator_mixin)
        module GoalEvaluator_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::talker::GoalEvaluator']
            end

            def ice_id(current=nil)
                '::talker::GoalEvaluator'
            end

            #
            # Operation signatures.
            #
            # def setTask(task, current=nil)
            # def getData(sender, current=nil)
            # def subscribe(subscriber, current=nil)
            # def unsubscribe(subscriber, current=nil)

            def inspect
                ::Ice::__stringify(self, T_GoalEvaluator)
            end
        end
        class GoalEvaluator
            include GoalEvaluator_mixin
        end
        module GoalEvaluatorPrx_mixin

            def setTask(task, _ctx=nil)
                GoalEvaluator_mixin::OP_setTask.invoke(self, [task], _ctx)
            end

            def getData(sender, _ctx=nil)
                GoalEvaluator_mixin::OP_getData.invoke(self, [sender], _ctx)
            end

            def subscribe(subscriber, _ctx=nil)
                GoalEvaluator_mixin::OP_subscribe.invoke(self, [subscriber], _ctx)
            end

            def unsubscribe(subscriber, _ctx=nil)
                GoalEvaluator_mixin::OP_unsubscribe.invoke(self, [subscriber], _ctx)
            end
        end
        class GoalEvaluatorPrx < ::Ice::ObjectPrx
            include GoalEvaluatorPrx_mixin

            def GoalEvaluatorPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::talker::GoalEvaluator', facetOrCtx, _ctx)
            end

            def GoalEvaluatorPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Talker::T_GoalEvaluator)
            T_GoalEvaluator = ::Ice::__declareClass('::talker::GoalEvaluator')
            T_GoalEvaluatorPrx = ::Ice::__declareProxy('::talker::GoalEvaluator')
        end

        T_GoalEvaluator.defineClass(GoalEvaluator, true, nil, [], [])
        GoalEvaluator_mixin::ICE_TYPE = T_GoalEvaluator

        T_GoalEvaluatorPrx.defineProxy(GoalEvaluatorPrx, T_GoalEvaluator)
        GoalEvaluatorPrx::ICE_TYPE = T_GoalEvaluatorPrx

        GoalEvaluator_mixin::OP_setTask = ::Ice::__defineOperation('setTask', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_GoalEvaluatorTask], [], ::Ice::T_int, [::Orca::T_BusyException, ::Orca::T_RequiredInterfaceFailedException])
        GoalEvaluator_mixin::OP_getData = ::Ice::__defineOperation('getData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Ice::T_string], [], ::Talker::T_GoalEvaluatorResult, [])
        GoalEvaluator_mixin::OP_subscribe = ::Ice::__defineOperation('subscribe', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Talker::T_GoalEvaluatorConsumerPrx], [], nil, [::Orca::T_SubscriptionFailedException])
        GoalEvaluator_mixin::OP_unsubscribe = ::Ice::__defineOperation('unsubscribe', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Talker::T_GoalEvaluatorConsumerPrx], [], nil, [])
    end
end
