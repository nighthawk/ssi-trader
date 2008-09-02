# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `pathfollower2d.ice'

require 'Ice'
require 'orca/common.rb'
require 'orca/bros1.rb'

module Orca

    if not defined?(::Orca::Waypoint2d)
        class Waypoint2d
            def initialize(target=::Orca::Frame2d.new, distanceTolerance=0.0, headingTolerance=0.0, timeTarget=::Orca::Time.new, maxApproachSpeed=0.0, maxApproachTurnrate=0.0)
                @target = target
                @distanceTolerance = distanceTolerance
                @headingTolerance = headingTolerance
                @timeTarget = timeTarget
                @maxApproachSpeed = maxApproachSpeed
                @maxApproachTurnrate = maxApproachTurnrate
            end

            def hash
                _h = 0
                _h = 5 * _h + @target.hash
                _h = 5 * _h + @distanceTolerance.hash
                _h = 5 * _h + @headingTolerance.hash
                _h = 5 * _h + @timeTarget.hash
                _h = 5 * _h + @maxApproachSpeed.hash
                _h = 5 * _h + @maxApproachTurnrate.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @target != other.target or
                    @distanceTolerance != other.distanceTolerance or
                    @headingTolerance != other.headingTolerance or
                    @timeTarget != other.timeTarget or
                    @maxApproachSpeed != other.maxApproachSpeed or
                    @maxApproachTurnrate != other.maxApproachTurnrate
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Waypoint2d)
            end

            attr_accessor :target, :distanceTolerance, :headingTolerance, :timeTarget, :maxApproachSpeed, :maxApproachTurnrate
        end

        T_Waypoint2d = ::Ice::__defineStruct('::orca::Waypoint2d', Waypoint2d, [
            ["target", ::Orca::T_Frame2d],
            ["distanceTolerance", ::Ice::T_float],
            ["headingTolerance", ::Ice::T_float],
            ["timeTarget", ::Orca::T_Time],
            ["maxApproachSpeed", ::Ice::T_float],
            ["maxApproachTurnrate", ::Ice::T_float]
        ])
    end

    if not defined?(::Orca::T_Path2d)
        T_Path2d = ::Ice::__defineSequence('::orca::Path2d', ::Orca::T_Waypoint2d)
    end

    if not defined?(::Orca::PathFollower2dData)
        class PathFollower2dData
            def initialize(timeStamp=::Orca::Time.new, path=nil)
                @timeStamp = timeStamp
                @path = path
            end

            def hash
                _h = 0
                _h = 5 * _h + @timeStamp.hash
                _h = 5 * _h + @path.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @timeStamp != other.timeStamp or
                    @path != other.path
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PathFollower2dData)
            end

            attr_accessor :timeStamp, :path
        end

        T_PathFollower2dData = ::Ice::__defineStruct('::orca::PathFollower2dData', PathFollower2dData, [
            ["timeStamp", ::Orca::T_Time],
            ["path", ::Orca::T_Path2d]
        ])
    end

    if not defined?(::Orca::PathFollower2dConsumer_mixin)
        module PathFollower2dConsumer_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::orca::PathFollower2dConsumer']
            end

            def ice_id(current=nil)
                '::orca::PathFollower2dConsumer'
            end

            #
            # Operation signatures.
            #
            # def setWaypointIndex(index, current=nil)
            # def setActivationTime(absoluteTime, relativeTime, current=nil)
            # def setData(data, current=nil)
            # def setEnabledState(enabledState, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathFollower2dConsumer)
            end
        end
        class PathFollower2dConsumer
            include PathFollower2dConsumer_mixin
        end
        module PathFollower2dConsumerPrx_mixin

            def setWaypointIndex(index, _ctx=nil)
                PathFollower2dConsumer_mixin::OP_setWaypointIndex.invoke(self, [index], _ctx)
            end

            def setActivationTime(absoluteTime, relativeTime, _ctx=nil)
                PathFollower2dConsumer_mixin::OP_setActivationTime.invoke(self, [absoluteTime, relativeTime], _ctx)
            end

            def setData(data, _ctx=nil)
                PathFollower2dConsumer_mixin::OP_setData.invoke(self, [data], _ctx)
            end

            def setEnabledState(enabledState, _ctx=nil)
                PathFollower2dConsumer_mixin::OP_setEnabledState.invoke(self, [enabledState], _ctx)
            end
        end
        class PathFollower2dConsumerPrx < ::Ice::ObjectPrx
            include PathFollower2dConsumerPrx_mixin

            def PathFollower2dConsumerPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::orca::PathFollower2dConsumer', facetOrCtx, _ctx)
            end

            def PathFollower2dConsumerPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Orca::T_PathFollower2dConsumer)
            T_PathFollower2dConsumer = ::Ice::__declareClass('::orca::PathFollower2dConsumer')
            T_PathFollower2dConsumerPrx = ::Ice::__declareProxy('::orca::PathFollower2dConsumer')
        end

        T_PathFollower2dConsumer.defineClass(PathFollower2dConsumer, true, nil, [], [])
        PathFollower2dConsumer_mixin::ICE_TYPE = T_PathFollower2dConsumer

        T_PathFollower2dConsumerPrx.defineProxy(PathFollower2dConsumerPrx, T_PathFollower2dConsumer)
        PathFollower2dConsumerPrx::ICE_TYPE = T_PathFollower2dConsumerPrx

        PathFollower2dConsumer_mixin::OP_setWaypointIndex = ::Ice::__defineOperation('setWaypointIndex', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Ice::T_int], [], nil, [])
        PathFollower2dConsumer_mixin::OP_setActivationTime = ::Ice::__defineOperation('setActivationTime', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Orca::T_Time, ::Ice::T_double], [], nil, [])
        PathFollower2dConsumer_mixin::OP_setData = ::Ice::__defineOperation('setData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Orca::T_PathFollower2dData], [], nil, [])
        PathFollower2dConsumer_mixin::OP_setEnabledState = ::Ice::__defineOperation('setEnabledState', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Ice::T_bool], [], nil, [])
    end

    if not defined?(::Orca::PathFollower2d_mixin)
        module PathFollower2d_mixin
            include ::Ice::Object_mixin

            def ice_ids(current=nil)
                ['::Ice::Object', '::orca::PathFollower2d']
            end

            def ice_id(current=nil)
                '::orca::PathFollower2d'
            end

            #
            # Operation signatures.
            #
            # def getData(current=nil)
            # def setData(path, activateImmediately, current=nil)
            # def activateNow(current=nil)
            # def getWaypointIndex(current=nil)
            # def getAbsoluteActivationTime(current=nil)
            # def getRelativeActivationTime(current=nil)
            # def setEnabled(enabled, current=nil)
            # def enabled(current=nil)
            # def subscribe(subscriber, current=nil)
            # def unsubscribe(subscriber, current=nil)

            def inspect
                ::Ice::__stringify(self, T_PathFollower2d)
            end
        end
        class PathFollower2d
            include PathFollower2d_mixin
        end
        module PathFollower2dPrx_mixin

            def getData(_ctx=nil)
                PathFollower2d_mixin::OP_getData.invoke(self, [], _ctx)
            end

            def setData(path, activateImmediately, _ctx=nil)
                PathFollower2d_mixin::OP_setData.invoke(self, [path, activateImmediately], _ctx)
            end

            def activateNow(_ctx=nil)
                PathFollower2d_mixin::OP_activateNow.invoke(self, [], _ctx)
            end

            def getWaypointIndex(_ctx=nil)
                PathFollower2d_mixin::OP_getWaypointIndex.invoke(self, [], _ctx)
            end

            def getAbsoluteActivationTime(_ctx=nil)
                PathFollower2d_mixin::OP_getAbsoluteActivationTime.invoke(self, [], _ctx)
            end

            def getRelativeActivationTime(_ctx=nil)
                PathFollower2d_mixin::OP_getRelativeActivationTime.invoke(self, [], _ctx)
            end

            def setEnabled(enabled, _ctx=nil)
                PathFollower2d_mixin::OP_setEnabled.invoke(self, [enabled], _ctx)
            end

            def enabled(_ctx=nil)
                PathFollower2d_mixin::OP_enabled.invoke(self, [], _ctx)
            end

            def subscribe(subscriber, _ctx=nil)
                PathFollower2d_mixin::OP_subscribe.invoke(self, [subscriber], _ctx)
            end

            def unsubscribe(subscriber, _ctx=nil)
                PathFollower2d_mixin::OP_unsubscribe.invoke(self, [subscriber], _ctx)
            end
        end
        class PathFollower2dPrx < ::Ice::ObjectPrx
            include PathFollower2dPrx_mixin

            def PathFollower2dPrx.checkedCast(proxy, facetOrCtx=nil, _ctx=nil)
                ice_checkedCast(proxy, '::orca::PathFollower2d', facetOrCtx, _ctx)
            end

            def PathFollower2dPrx.uncheckedCast(proxy, facet=nil)
                ice_uncheckedCast(proxy, facet)
            end
        end

        if not defined?(::Orca::T_PathFollower2d)
            T_PathFollower2d = ::Ice::__declareClass('::orca::PathFollower2d')
            T_PathFollower2dPrx = ::Ice::__declareProxy('::orca::PathFollower2d')
        end

        T_PathFollower2d.defineClass(PathFollower2d, true, nil, [], [])
        PathFollower2d_mixin::ICE_TYPE = T_PathFollower2d

        T_PathFollower2dPrx.defineProxy(PathFollower2dPrx, T_PathFollower2d)
        PathFollower2dPrx::ICE_TYPE = T_PathFollower2dPrx

        PathFollower2d_mixin::OP_getData = ::Ice::__defineOperation('getData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], ::Orca::T_PathFollower2dData, [])
        PathFollower2d_mixin::OP_setData = ::Ice::__defineOperation('setData', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Orca::T_PathFollower2dData, ::Ice::T_bool], [], nil, [::Orca::T_MalformedParametersException, ::Orca::T_OrcaException])
        PathFollower2d_mixin::OP_activateNow = ::Ice::__defineOperation('activateNow', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], nil, [])
        PathFollower2d_mixin::OP_getWaypointIndex = ::Ice::__defineOperation('getWaypointIndex', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], ::Ice::T_int, [])
        PathFollower2d_mixin::OP_getAbsoluteActivationTime = ::Ice::__defineOperation('getAbsoluteActivationTime', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [::Orca::T_Time], ::Ice::T_bool, [])
        PathFollower2d_mixin::OP_getRelativeActivationTime = ::Ice::__defineOperation('getRelativeActivationTime', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [::Ice::T_double], ::Ice::T_bool, [])
        PathFollower2d_mixin::OP_setEnabled = ::Ice::__defineOperation('setEnabled', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Ice::T_bool], [], nil, [])
        PathFollower2d_mixin::OP_enabled = ::Ice::__defineOperation('enabled', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [], [], ::Ice::T_bool, [])
        PathFollower2d_mixin::OP_subscribe = ::Ice::__defineOperation('subscribe', ::Ice::OperationMode::Normal, ::Ice::OperationMode::Normal, false, [::Orca::T_PathFollower2dConsumerPrx], [], nil, [::Orca::T_SubscriptionFailedException])
        PathFollower2d_mixin::OP_unsubscribe = ::Ice::__defineOperation('unsubscribe', ::Ice::OperationMode::Idempotent, ::Ice::OperationMode::Idempotent, false, [::Orca::T_PathFollower2dConsumerPrx], [], nil, [])
    end
end
