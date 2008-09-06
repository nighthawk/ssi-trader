/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA_CONTAINERS_ICE
#define ORCA_CONTAINERS_ICE


module orca
{

// The following were copied from <Ice/BuiltinSequences.ice>
// Use them for all sequences of basic types.
// We duplicate it inside the orca module for convenience.

//! A sequence of bools.
sequence<bool> BoolSeq;

//! A sequence of bytes.
sequence<byte> ByteSeq;

//! A sequence of shorts.
sequence<short> ShortSeq;

//! A sequence of ints.
sequence<int> IntSeq;

// A sequence of longs.
// sequence<long> LongSeq;

//! A sequence of floats.
sequence<float> FloatSeq;

//! A sequence of doubles.
sequence<double> DoubleSeq;

//! A sequence of strings.
sequence<string> StringSeq;

// A sequence of objects.
// sequence<Object> ObjectSeq;
    
// A sequence of object proxies.
// sequence<Object*> ObjectProxySeq;


// this is a very common dictionary

//! A mapping from one string to another.
dictionary<string,string> StringStringDict;

}; // module

#endif
