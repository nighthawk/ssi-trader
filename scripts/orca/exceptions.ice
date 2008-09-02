/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2008 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

#ifndef ORCA2_EXCEPTIONS_ICE
#define ORCA2_EXCEPTIONS_ICE

/*!
    @brief Defines cross-platform interfaces and data objects.
    @namespace orca

    This namespace defines the core reusable part of Orca: interfaces and objects.
*/
module orca
{

/*!
    @brief A generic run-time exception.
    
    Orca Components can use this to signal error conditions to their clients.

    To catch this exception and all derived ones, do the following:
@verbatim
try
{
    // make a remote call
}
catch ( const orca::OrcaException& e )
{
    cout << e << ": " << e.what <<endl;
    // do something else
}
@endverbatim
*/
exception OrcaException
{
    //! Error description.
    string what;
};

//! Server failed to configure itself as requrested by client.
exception ConfigurationNotExistException extends OrcaException {};

/*!
    Raised when the server does not have the requested data.

    Typically, this is because the server has not fully initialized yet.
*/
exception DataNotExistException extends OrcaException {};

//! Indicates a problem with robot hardware, e.g. sensors and actuators.
exception HardwareFailedException extends OrcaException {};

//! Raised when the server fails to subscribe client for periodic updates.
exception SubscriptionFailedException extends OrcaException {};

//! Raised when the component is too busy to process requests.
exception BusyException extends OrcaException {};

//! Raised when a component failed to connect to a required interface
exception RequiredInterfaceFailedException extends OrcaException {};

//! Raised when one or more parameters to a remote request were malformed.
exception MalformedParametersException extends OrcaException {};

//! Raised when a remote operation is not implemented.
exception OperationNotImplementedException extends OrcaException {};

}; // module

#endif
