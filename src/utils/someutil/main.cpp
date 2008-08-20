/*
 * Orca Project: Components for robotics 
 *               http://orca-robotics.sf.net/
 * Copyright (c) 2004-2006 Alex Brooks, Alexei Makarenko, Tobias Kaupp
 *
 * This copy of Orca is licensed to you under the terms described in the
 * ORCA_LICENSE file included in this distribution.
 *
 */

// make sure we can include both common and project-specific interfaces.
#include <orca/home.h>
#include <talker/someinterface.h>
#include <orcaice/orcaice.h>

int
main(int argc, char* argv[])
{
    // Orca Slice type
    orca::HomeData homeData;
        
    // project-specific Slice type (struct)
    talker::SomeStructData someSData;
    someSData.count++;
                
    // project-specific Slice type (class)
    talker::SomeClassDataPtr someCData = new talker::SomeClassData;
                        
    // orca function with project-specific Slice type
    orcaice::setToNow( someCData->timeStamp );
                                
    return 0;
}
