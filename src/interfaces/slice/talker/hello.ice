#ifndef TALKER_HELLO_ICE
#define TALKER_HELLO_ICE

module talker
{
/*!
    @ingroup ep_interfaces
    @defgroup ep_interface_hello Hello
    @brief Hello world example

Ths is an example interface definition written in Slice IDL.

    @{
*/

//! Interface to some device or algorithm.
interface Hello
{
    //! Set current state
    void setMessage( string message );
};

/*! @} */
}; // module

#endif
