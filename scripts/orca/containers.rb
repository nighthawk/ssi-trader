# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `containers.ice'

require 'Ice'

module Orca

    if not defined?(::Orca::T_BoolSeq)
        T_BoolSeq = ::Ice::__defineSequence('::orca::BoolSeq', ::Ice::T_bool)
    end

    if not defined?(::Orca::T_ByteSeq)
        T_ByteSeq = ::Ice::__defineSequence('::orca::ByteSeq', ::Ice::T_byte)
    end

    if not defined?(::Orca::T_ShortSeq)
        T_ShortSeq = ::Ice::__defineSequence('::orca::ShortSeq', ::Ice::T_short)
    end

    if not defined?(::Orca::T_IntSeq)
        T_IntSeq = ::Ice::__defineSequence('::orca::IntSeq', ::Ice::T_int)
    end

    if not defined?(::Orca::T_FloatSeq)
        T_FloatSeq = ::Ice::__defineSequence('::orca::FloatSeq', ::Ice::T_float)
    end

    if not defined?(::Orca::T_DoubleSeq)
        T_DoubleSeq = ::Ice::__defineSequence('::orca::DoubleSeq', ::Ice::T_double)
    end

    if not defined?(::Orca::T_StringSeq)
        T_StringSeq = ::Ice::__defineSequence('::orca::StringSeq', ::Ice::T_string)
    end

    if not defined?(::Orca::T_StringStringDict)
        T_StringStringDict = ::Ice::__defineDictionary('::orca::StringStringDict', ::Ice::T_string, ::Ice::T_string)
    end
end
