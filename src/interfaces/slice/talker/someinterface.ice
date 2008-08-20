#ifndef TALKER_SOME_INTERFACE_ICE
#define TALKER_SOME_INTERFACE_ICE

#include <orca/common.ice>

module talker
{
/*!
    @ingroup ep_interfaces
    @defgroup ep_interface_someinterface SomeInterface
    @brief Example interface definition.

Ths is an example interface definition written in Slice IDL.

    @{
*/

//! A simple struct.
struct SomeStructData
{
    //! Some information
    int count;
};

//! A class deriving from Orca standard object.
//! Notice reference to Orca module.
class SomeClassData
{
    //! Time when the object was observed, created, etc.
    orca::Time timeStamp;

    //! Some information
    int count;
};

//! Interface to some device or algorithm.
interface SomeInterface
{
    //! Get current state
    idempotent bool getState();

    //! Set current state
    void setState( bool state );
};

/*! @} */
}; // module

#endif
