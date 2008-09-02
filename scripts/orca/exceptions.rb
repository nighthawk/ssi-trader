# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `exceptions.ice'

require 'Ice'

module Orca

    if not defined?(::Orca::OrcaException)
        class OrcaException < Ice::UserException
            def initialize(what='')
                @what = what
            end

            def to_s
                'orca::OrcaException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end

            attr_accessor :what
        end

        T_OrcaException = ::Ice::__defineException('::orca::OrcaException', OrcaException, nil, [["what", ::Ice::T_string]])
        OrcaException::ICE_TYPE = T_OrcaException
    end

    if not defined?(::Orca::ConfigurationNotExistException)
        class ConfigurationNotExistException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::ConfigurationNotExistException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_ConfigurationNotExistException = ::Ice::__defineException('::orca::ConfigurationNotExistException', ConfigurationNotExistException, ::Orca::T_OrcaException, [])
        ConfigurationNotExistException::ICE_TYPE = T_ConfigurationNotExistException
    end

    if not defined?(::Orca::DataNotExistException)
        class DataNotExistException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::DataNotExistException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_DataNotExistException = ::Ice::__defineException('::orca::DataNotExistException', DataNotExistException, ::Orca::T_OrcaException, [])
        DataNotExistException::ICE_TYPE = T_DataNotExistException
    end

    if not defined?(::Orca::HardwareFailedException)
        class HardwareFailedException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::HardwareFailedException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_HardwareFailedException = ::Ice::__defineException('::orca::HardwareFailedException', HardwareFailedException, ::Orca::T_OrcaException, [])
        HardwareFailedException::ICE_TYPE = T_HardwareFailedException
    end

    if not defined?(::Orca::SubscriptionFailedException)
        class SubscriptionFailedException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::SubscriptionFailedException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_SubscriptionFailedException = ::Ice::__defineException('::orca::SubscriptionFailedException', SubscriptionFailedException, ::Orca::T_OrcaException, [])
        SubscriptionFailedException::ICE_TYPE = T_SubscriptionFailedException
    end

    if not defined?(::Orca::BusyException)
        class BusyException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::BusyException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_BusyException = ::Ice::__defineException('::orca::BusyException', BusyException, ::Orca::T_OrcaException, [])
        BusyException::ICE_TYPE = T_BusyException
    end

    if not defined?(::Orca::RequiredInterfaceFailedException)
        class RequiredInterfaceFailedException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::RequiredInterfaceFailedException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_RequiredInterfaceFailedException = ::Ice::__defineException('::orca::RequiredInterfaceFailedException', RequiredInterfaceFailedException, ::Orca::T_OrcaException, [])
        RequiredInterfaceFailedException::ICE_TYPE = T_RequiredInterfaceFailedException
    end

    if not defined?(::Orca::MalformedParametersException)
        class MalformedParametersException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::MalformedParametersException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_MalformedParametersException = ::Ice::__defineException('::orca::MalformedParametersException', MalformedParametersException, ::Orca::T_OrcaException, [])
        MalformedParametersException::ICE_TYPE = T_MalformedParametersException
    end

    if not defined?(::Orca::OperationNotImplementedException)
        class OperationNotImplementedException < ::Orca::OrcaException
            def initialize(what='')
                super(what)
            end

            def to_s
                'orca::OperationNotImplementedException'
            end

            def inspect
                return ::Ice::__stringifyException(self)
            end
        end

        T_OperationNotImplementedException = ::Ice::__defineException('::orca::OperationNotImplementedException', OperationNotImplementedException, ::Orca::T_OrcaException, [])
        OperationNotImplementedException::ICE_TYPE = T_OperationNotImplementedException
    end
end
