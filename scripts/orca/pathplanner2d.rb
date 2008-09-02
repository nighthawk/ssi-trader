# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `pathplanner2d.ice'

require 'Ice'
require 'orca/bros1.rb'
require 'orca/pathfollower2d.rb'

module Orca

    if not defined?(::Orca::PathPlanner2dResult)
        class PathPlanner2dResult
            include Comparable

            def initialize(val)
                fail("invalid value #{val} for PathPlanner2dResult") unless(val >= 0 and val < 5)
                @val = val
            end

            def PathPlanner2dResult.from_int(val)
                raise IndexError, "#{val} is out of range 0..4" if(val < 0 || val > 4)
                @@_values[val]
            end

            def to_s
                @@_names[@val]
            end

            def to_i
                @val
            end

            def <=>(other)
                other.is_a?(PathPlanner2dResult) or raise ArgumentError, "value must be a PathPlanner2dResult"
                @val <=> other.to_i
            end

            def hash
                @val.hash
            end

            def inspect
                @@_names[@val] + "(#{@val})"
            end

            def PathPlanner2dResult.each(&block)
                @@_values.each(&block)
            end

            @@_names = ['PathOk', 'PathStartNotValid', 'PathDestinationNotValid', 'PathDestinationUnreachable', 'PathOtherError']
            @@_values = [PathPlanner2dResult.new(0), PathPlanner2dResult.new(1), PathPlanner2dResult.new(2), PathPlanner2dResult.new(3), PathPlanner2dResult.new(4)]

            PathOk = @@_values[0]
            PathStartNotValid = @@_values[1]
            PathDestinationNotValid = @@_values[2]
            PathDestinationUnreachable = @@_values[3]
            PathOtherError = @@_values[4]

            private_class_method :new
        end

        T_PathPlanner2dResult = ::Ice::__defineEnum('::orca::PathPlanner2dResult', PathPlanner2dResult, [PathPlanner2dResult::PathOk, PathPlanner2dResult::PathStartNotValid, PathPlanner2dResult::PathDestinationNotValid, PathPlanner2dResult::PathDestinationUnreachable, PathPlanner2dResult::PathOtherError])
    end

    if not defined?(::Orca::PathPlanner2dData)
        class PathPlanner2dData
            def initialize(timeStamp=::Orca::Time.new, path=nil, result=::Orca::PathPlanner2dResult::PathOk, resultDescription='')
                @timeStamp = timeStamp
                @path = path
                @result = result
                @resultDescription = resultDescription
            end

            def hash
                _h = 0
                _h = 5 * _h + @timeStamp.hash
                _h = 5 * _h + @path.hash
                _h = 5 * _h + @result.hash
                _h = 5 * _h + @resultDescription.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @timeStamp != other.timeStamp or
                    @path != other.path or
                    @result != other.result or
                    @resultDescription != other.resultDescription
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PathPlanner2dData)
            end

            attr_accessor :timeStamp, :path, :result, :resultDescription
        end

        T_PathPlanner2dData = ::Ice::__defineStruct('::orca::PathPlanner2dData', PathPlanner2dData, [
            ["timeStamp", ::Orca::T_Time],
            ["path", ::Orca::T_Path2d],
            ["result", ::Orca::T_PathPlanner2dResult],
            ["resultDescription", ::Ice::T_string]
        ])
    end

    if not defined?(::Orca::PathPlanner2dConsumer_mixin)
        module PathPlanner2dConsumer_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::orca::PathPlanner2dConsumer']
            end

            def ice_id(current=nil)
                '::orca::PathPlanner2dConsumer'
            end

            #
            # Operation signatures.
            #
            # def setData(obj, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathPlanner2dConsumer)
            end
        end
        class PathPlanner2dConsumer
            include PathPlanner2dConsumer_mixin
        end
        module PathPlanner2dConsumerPrx_mixin

            def setData(obj, _ctx=nil)
                PathPlanner2dConsumer_mixin::OP_setData.invoke(self, [obj], _ctx)
            end
        end
        class PathPlanner2dConsumerPrx < ::Ice::ObjectPrx
            include PathPlanner2dConsumerPrx_mixin

            def PathPlanner2dConsumerPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::orca::PathPlanner2dConsumer', facetOrCtx, _ctx)
            end

            def PathPlanner2dConsumerPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Orca::T_PathPlanner2dConsumer)
            T_PathPlanner2dConsumer = ::Ice::__declareClass('::orca::PathPlanner2dConsumer')
            T_PathPlanner2dConsumerPrx = ::Ice::__declareProxy('::orca::PathPlanner2dConsumer')
        end

        T_PathPlanner2dConsumer.defineClass(PathPlanner2dConsumer, true, nil, [], [])
        PathPlanner2dConsumer_mixin::ICE_TYPE = T_PathPlanner2dConsumer

        T_PathPlanner2dConsumerPrx.defineProxy(PathPlanner2dConsumerPrx, T_PathPlanner2dConsumer)
        PathPlanner2dConsumerPrx::ICE_TYPE = T_PathPlanner2dConsumerPrx

        PathPlanner2dConsumer_mixin::OP_setData = ::Ice::__defineOperation('setData', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Orca::T_PathPlanner2dData], [], nil, [])
    end

    if not defined?(::Orca::PathPlanner2dTask)
        class PathPlanner2dTask
            def initialize(timeStamp=::Orca::Time.new, coarsePath=nil, prx=nil)
                @timeStamp = timeStamp
                @coarsePath = coarsePath
                @prx = prx
            end

            def hash
                _h = 0
                _h = 5 * _h + @timeStamp.hash
                _h = 5 * _h + @coarsePath.hash
                _h = 5 * _h + @prx.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @timeStamp != other.timeStamp or
                    @coarsePath != other.coarsePath or
                    @prx != other.prx
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PathPlanner2dTask)
            end

            attr_accessor :timeStamp, :coarsePath, :prx
        end

        T_PathPlanner2dTask = ::Ice::__defineStruct('::orca::PathPlanner2dTask', PathPlanner2dTask, [
            ["timeStamp", ::Orca::T_Time],
            ["coarsePath", ::Orca::T_Path2d],
            ["prx", ::Orca::T_PathPlanner2dConsumerPrx]
        ])
    end

    if not defined?(::Orca::PathPlanner2d_mixin)
        module PathPlanner2d_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::orca::PathPlanner2d']
            end

            def ice_id(current=nil)
                '::orca::PathPlanner2d'
            end

            #
            # Operation signatures.
            #
            # def setTask(task, current=nil)
            # def getData(current=nil)
            # def subscribe(subscriber, current=nil)
            # def unsubscribe(subscriber, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathPlanner2d)
            end
        end
        class PathPlanner2d
            include PathPlanner2d_mixin
        end
        module PathPlanner2dPrx_mixin

            def setTask(task, _ctx=nil)
                PathPlanner2d_mixin::OP_setTask.invoke(self, [task], _ctx)
            end

            def getData(_ctx=nil)
                PathPlanner2d_mixin::OP_getData.invoke(self, [], _ctx)
            end

            def subscribe(subscriber, _ctx=nil)
                PathPlanner2d_mixin::OP_subscribe.invoke(self, [subscriber], _ctx)
            end

            def unsubscribe(subscriber, _ctx=nil)
                PathPlanner2d_mixin::OP_unsubscribe.invoke(self, [subscriber], _ctx)
            end
        end
        class PathPlanner2dPrx < ::Ice::ObjectPrx
            include PathPlanner2dPrx_mixin

            def PathPlanner2dPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::orca::PathPlanner2d', facetOrCtx, _ctx)
            end

            def PathPlanner2dPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Orca::T_PathPlanner2d)
            T_PathPlanner2d = ::Ice::__declareClass('::orca::PathPlanner2d')
            T_PathPlanner2dPrx = ::Ice::__declareProxy('::orca::PathPlanner2d')
        end

        T_PathPlanner2d.defineClass(PathPlanner2d, true, nil, [], [])
        PathPlanner2d_mixin::ICE_TYPE = T_PathPlanner2d

        T_PathPlanner2dPrx.defineProxy(PathPlanner2dPrx, T_PathPlanner2d)
        PathPlanner2dPrx::ICE_TYPE = T_PathPlanner2dPrx

        PathPlanner2d_mixin::OP_setTask = ::Ice::__defineOperation('setTask', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Orca::T_PathPlanner2dTask], [], ::Ice::T_int, [::Orca::T_BusyException, ::Orca::T_RequiredInterfaceFailedException])
        PathPlanner2d_mixin::OP_getData = ::Ice::__defineOperation('getData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], ::Orca::T_PathPlanner2dData, [])
        PathPlanner2d_mixin::OP_subscribe = ::Ice::__defineOperation('subscribe', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Orca::T_PathPlanner2dConsumerPrx], [], nil, [::Orca::T_SubscriptionFailedException])
        PathPlanner2d_mixin::OP_unsubscribe = ::Ice::__defineOperation('unsubscribe', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Orca::T_PathPlanner2dConsumerPrx], [], nil, [])
    end
end
