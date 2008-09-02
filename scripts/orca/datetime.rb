# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `datetime.ice'

require 'Ice'

module Orca

    if not defined?(::Orca::Time)
        class Time
            def initialize(seconds=0, useconds=0)
                @seconds = seconds
                @useconds = useconds
            end

            def hash
                _h = 0
                _h = 5 * _h + @seconds.hash
                _h = 5 * _h + @useconds.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @seconds != other.seconds or
                    @useconds != other.useconds
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Time)
            end

            attr_accessor :seconds, :useconds
        end

        T_Time = ::Ice::__defineStruct('::orca::Time', Time, [
            ["seconds", ::Ice::T_int],
            ["useconds", ::Ice::T_int]
        ])
    end

    if not defined?(::Orca::TimeOfDay)
        class TimeOfDay
            def initialize(hours=0, minutes=0, seconds=0.0)
                @hours = hours
                @minutes = minutes
                @seconds = seconds
            end

            def hash
                _h = 0
                _h = 5 * _h + @hours.hash
                _h = 5 * _h + @minutes.hash
                _h = 5 * _h + @seconds.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @hours != other.hours or
                    @minutes != other.minutes or
                    @seconds != other.seconds
                true
            end

            def inspect
                ::Ice::__stringify(self, T_TimeOfDay)
            end

            attr_accessor :hours, :minutes, :seconds
        end

        T_TimeOfDay = ::Ice::__defineStruct('::orca::TimeOfDay', TimeOfDay, [
            ["hours", ::Ice::T_int],
            ["minutes", ::Ice::T_int],
            ["seconds", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Date)
        class Date
            def initialize(day=0, month=0, year=0)
                @day = day
                @month = month
                @year = year
            end

            def hash
                _h = 0
                _h = 5 * _h + @day.hash
                _h = 5 * _h + @month.hash
                _h = 5 * _h + @year.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @day != other.day or
                    @month != other.month or
                    @year != other.year
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Date)
            end

            attr_accessor :day, :month, :year
        end

        T_Date = ::Ice::__defineStruct('::orca::Date', Date, [
            ["day", ::Ice::T_int],
            ["month", ::Ice::T_int],
            ["year", ::Ice::T_int]
        ])
    end
end
