# **********************************************************************
#
# Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

# Ice version 3.2.1
# Generated from file `bros1.ice'

require 'Ice'

module Orca

    if not defined?(::Orca::CartesianPoint2d)
        class CartesianPoint2d
            def initialize(x=0.0, y=0.0)
                @x = x
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianPoint2d)
            end

            attr_accessor :x, :y
        end

        T_CartesianPoint2d = ::Ice::__defineStruct('::orca::CartesianPoint2d', CartesianPoint2d, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::CartesianVelocity2d)
        class CartesianVelocity2d
            def initialize(x=0.0, y=0.0)
                @x = x
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianVelocity2d)
            end

            attr_accessor :x, :y
        end

        T_CartesianVelocity2d = ::Ice::__defineStruct('::orca::CartesianVelocity2d', CartesianVelocity2d, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::CartesianAcceleration2d)
        class CartesianAcceleration2d
            def initialize(x=0.0, y=0.0)
                @x = x
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianAcceleration2d)
            end

            attr_accessor :x, :y
        end

        T_CartesianAcceleration2d = ::Ice::__defineStruct('::orca::CartesianAcceleration2d', CartesianAcceleration2d, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::PolarPoint2d)
        class PolarPoint2d
            def initialize(r=0.0, o=0.0)
                @r = r
                @o = o
            end

            def hash
                _h = 0
                _h = 5 * _h + @r.hash
                _h = 5 * _h + @o.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @r != other.r or
                    @o != other.o
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PolarPoint2d)
            end

            attr_accessor :r, :o
        end

        T_PolarPoint2d = ::Ice::__defineStruct('::orca::PolarPoint2d', PolarPoint2d, [
            ["r", ::Ice::T_double],
            ["o", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::PolarVelocity2d)
        class PolarVelocity2d
            def initialize(r=0.0, o=0.0)
                @r = r
                @o = o
            end

            def hash
                _h = 0
                _h = 5 * _h + @r.hash
                _h = 5 * _h + @o.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @r != other.r or
                    @o != other.o
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PolarVelocity2d)
            end

            attr_accessor :r, :o
        end

        T_PolarVelocity2d = ::Ice::__defineStruct('::orca::PolarVelocity2d', PolarVelocity2d, [
            ["r", ::Ice::T_double],
            ["o", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::PolarAcceleration2d)
        class PolarAcceleration2d
            def initialize(r=0.0, o=0.0)
                @r = r
                @o = o
            end

            def hash
                _h = 0
                _h = 5 * _h + @r.hash
                _h = 5 * _h + @o.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @r != other.r or
                    @o != other.o
                true
            end

            def inspect
                ::Ice::__stringify(self, T_PolarAcceleration2d)
            end

            attr_accessor :r, :o
        end

        T_PolarAcceleration2d = ::Ice::__defineStruct('::orca::PolarAcceleration2d', PolarAcceleration2d, [
            ["r", ::Ice::T_double],
            ["o", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Vector2d)
        class Vector2d
            def initialize(x=0.0, y=0.0)
                @x = x
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Vector2d)
            end

            attr_accessor :x, :y
        end

        T_Vector2d = ::Ice::__defineStruct('::orca::Vector2d', Vector2d, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Frame2d)
        class Frame2d
            def initialize(p=::Orca::CartesianPoint2d.new, o=0.0)
                @p = p
                @o = o
            end

            def hash
                _h = 0
                _h = 5 * _h + @p.hash
                _h = 5 * _h + @o.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @p != other.p or
                    @o != other.o
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Frame2d)
            end

            attr_accessor :p, :o
        end

        T_Frame2d = ::Ice::__defineStruct('::orca::Frame2d', Frame2d, [
            ["p", ::Orca::T_CartesianPoint2d],
            ["o", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Twist2d)
        class Twist2d
            def initialize(v=::Orca::CartesianVelocity2d.new, w=0.0)
                @v = v
                @w = w
            end

            def hash
                _h = 0
                _h = 5 * _h + @v.hash
                _h = 5 * _h + @w.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @v != other.v or
                    @w != other.w
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Twist2d)
            end

            attr_accessor :v, :w
        end

        T_Twist2d = ::Ice::__defineStruct('::orca::Twist2d', Twist2d, [
            ["v", ::Orca::T_CartesianVelocity2d],
            ["w", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Force2d)
        class Force2d
            def initialize(x=0.0, y=0.0)
                @x = x
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Force2d)
            end

            attr_accessor :x, :y
        end

        T_Force2d = ::Ice::__defineStruct('::orca::Force2d', Force2d, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Wrench2d)
        class Wrench2d
            def initialize(f=::Orca::Force2d.new, m=0.0)
                @f = f
                @m = m
            end

            def hash
                _h = 0
                _h = 5 * _h + @f.hash
                _h = 5 * _h + @m.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @f != other.f or
                    @m != other.m
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Wrench2d)
            end

            attr_accessor :f, :m
        end

        T_Wrench2d = ::Ice::__defineStruct('::orca::Wrench2d', Wrench2d, [
            ["f", ::Orca::T_Force2d],
            ["m", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Size2d)
        class Size2d
            def initialize(l=0.0, w=0.0)
                @l = l
                @w = w
            end

            def hash
                _h = 0
                _h = 5 * _h + @l.hash
                _h = 5 * _h + @w.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @l != other.l or
                    @w != other.w
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Size2d)
            end

            attr_accessor :l, :w
        end

        T_Size2d = ::Ice::__defineStruct('::orca::Size2d', Size2d, [
            ["l", ::Ice::T_double],
            ["w", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::DiscreteSize2d)
        class DiscreteSize2d
            def initialize(l=0, w=0)
                @l = l
                @w = w
            end

            def hash
                _h = 0
                _h = 5 * _h + @l.hash
                _h = 5 * _h + @w.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @l != other.l or
                    @w != other.w
                true
            end

            def inspect
                ::Ice::__stringify(self, T_DiscreteSize2d)
            end

            attr_accessor :l, :w
        end

        T_DiscreteSize2d = ::Ice::__defineStruct('::orca::DiscreteSize2d', DiscreteSize2d, [
            ["l", ::Ice::T_int],
            ["w", ::Ice::T_int]
        ])
    end

    if not defined?(::Orca::CartesianPoint)
        class CartesianPoint
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianPoint)
            end

            attr_accessor :x, :y, :z
        end

        T_CartesianPoint = ::Ice::__defineStruct('::orca::CartesianPoint', CartesianPoint, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::CartesianVector)
        class CartesianVector
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianVector)
            end

            attr_accessor :x, :y, :z
        end

        T_CartesianVector = ::Ice::__defineStruct('::orca::CartesianVector', CartesianVector, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::CartesianVelocity)
        class CartesianVelocity
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianVelocity)
            end

            attr_accessor :x, :y, :z
        end

        T_CartesianVelocity = ::Ice::__defineStruct('::orca::CartesianVelocity', CartesianVelocity, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::CartesianAcceleration)
        class CartesianAcceleration
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_CartesianAcceleration)
            end

            attr_accessor :x, :y, :z
        end

        T_CartesianAcceleration = ::Ice::__defineStruct('::orca::CartesianAcceleration', CartesianAcceleration, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::AngularVelocity)
        class AngularVelocity
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_AngularVelocity)
            end

            attr_accessor :x, :y, :z
        end

        T_AngularVelocity = ::Ice::__defineStruct('::orca::AngularVelocity', AngularVelocity, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::AngularAcceleration)
        class AngularAcceleration
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_AngularAcceleration)
            end

            attr_accessor :x, :y, :z
        end

        T_AngularAcceleration = ::Ice::__defineStruct('::orca::AngularAcceleration', AngularAcceleration, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Force)
        class Force
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Force)
            end

            attr_accessor :x, :y, :z
        end

        T_Force = ::Ice::__defineStruct('::orca::Force', Force, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Moment)
        class Moment
            def initialize(x=0.0, y=0.0, z=0.0)
                @x = x
                @y = y
                @z = z
            end

            def hash
                _h = 0
                _h = 5 * _h + @x.hash
                _h = 5 * _h + @y.hash
                _h = 5 * _h + @z.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @x != other.x or
                    @y != other.y or
                    @z != other.z
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Moment)
            end

            attr_accessor :x, :y, :z
        end

        T_Moment = ::Ice::__defineStruct('::orca::Moment', Moment, [
            ["x", ::Ice::T_double],
            ["y", ::Ice::T_double],
            ["z", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Twist3d)
        class Twist3d
            def initialize(v=::Orca::CartesianVelocity.new, w=::Orca::AngularVelocity.new)
                @v = v
                @w = w
            end

            def hash
                _h = 0
                _h = 5 * _h + @v.hash
                _h = 5 * _h + @w.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @v != other.v or
                    @w != other.w
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Twist3d)
            end

            attr_accessor :v, :w
        end

        T_Twist3d = ::Ice::__defineStruct('::orca::Twist3d', Twist3d, [
            ["v", ::Orca::T_CartesianVelocity],
            ["w", ::Orca::T_AngularVelocity]
        ])
    end

    if not defined?(::Orca::Wrench)
        class Wrench
            def initialize(f=::Orca::Force.new, m=::Orca::Moment.new)
                @f = f
                @m = m
            end

            def hash
                _h = 0
                _h = 5 * _h + @f.hash
                _h = 5 * _h + @m.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @f != other.f or
                    @m != other.m
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Wrench)
            end

            attr_accessor :f, :m
        end

        T_Wrench = ::Ice::__defineStruct('::orca::Wrench', Wrench, [
            ["f", ::Orca::T_Force],
            ["m", ::Orca::T_Moment]
        ])
    end

    if not defined?(::Orca::OrientationE)
        class OrientationE
            def initialize(r=0.0, p=0.0, y=0.0)
                @r = r
                @p = p
                @y = y
            end

            def hash
                _h = 0
                _h = 5 * _h + @r.hash
                _h = 5 * _h + @p.hash
                _h = 5 * _h + @y.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @r != other.r or
                    @p != other.p or
                    @y != other.y
                true
            end

            def inspect
                ::Ice::__stringify(self, T_OrientationE)
            end

            attr_accessor :r, :p, :y
        end

        T_OrientationE = ::Ice::__defineStruct('::orca::OrientationE', OrientationE, [
            ["r", ::Ice::T_double],
            ["p", ::Ice::T_double],
            ["y", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::Frame3d)
        class Frame3d
            def initialize(p=::Orca::CartesianPoint.new, o=::Orca::OrientationE.new)
                @p = p
                @o = o
            end

            def hash
                _h = 0
                _h = 5 * _h + @p.hash
                _h = 5 * _h + @o.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @p != other.p or
                    @o != other.o
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Frame3d)
            end

            attr_accessor :p, :o
        end

        T_Frame3d = ::Ice::__defineStruct('::orca::Frame3d', Frame3d, [
            ["p", ::Orca::T_CartesianPoint],
            ["o", ::Orca::T_OrientationE]
        ])
    end

    if not defined?(::Orca::Size3d)
        class Size3d
            def initialize(l=0.0, w=0.0, h=0.0)
                @l = l
                @w = w
                @h = h
            end

            def hash
                _h = 0
                _h = 5 * _h + @l.hash
                _h = 5 * _h + @w.hash
                _h = 5 * _h + @h.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @l != other.l or
                    @w != other.w or
                    @h != other.h
                true
            end

            def inspect
                ::Ice::__stringify(self, T_Size3d)
            end

            attr_accessor :l, :w, :h
        end

        T_Size3d = ::Ice::__defineStruct('::orca::Size3d', Size3d, [
            ["l", ::Ice::T_double],
            ["w", ::Ice::T_double],
            ["h", ::Ice::T_double]
        ])
    end

    if not defined?(::Orca::DiscreteSize3d)
        class DiscreteSize3d
            def initialize(l=0, w=0, h=0)
                @l = l
                @w = w
                @h = h
            end

            def hash
                _h = 0
                _h = 5 * _h + @l.hash
                _h = 5 * _h + @w.hash
                _h = 5 * _h + @h.hash
                _h % 0x7fffffff
            end

            def ==(other)
                return false if
                    @l != other.l or
                    @w != other.w or
                    @h != other.h
                true
            end

            def inspect
                ::Ice::__stringify(self, T_DiscreteSize3d)
            end

            attr_accessor :l, :w, :h
        end

        T_DiscreteSize3d = ::Ice::__defineStruct('::orca::DiscreteSize3d', DiscreteSize3d, [
            ["l", ::Ice::T_int],
            ["w", ::Ice::T_int],
            ["h", ::Ice::T_int]
        ])
    end
end
