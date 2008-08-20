/*
 *  Orca Project: Components for robotics.
 *
 *  Copyright (C) 2004-2006
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
