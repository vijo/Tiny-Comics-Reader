/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.rarx;

public class MarkHeader extends RARHeader {
    private static final byte[] oldRarHeader = { 0x52, 0x45, 0x7e, 0x5e };
    private static final byte[] rar2Header =   { 0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00 };
    private static final byte[] uniqueHeader = { 0x55, 0x6e, 0x69, 0x71, 0x75, 0x45, 0x21 }; // "UniquE!" in ASCII
		
    public MarkHeader(byte[] data, int offset) {
	super(data, offset);
	validateData();
    }
	
    protected void validateData() {
	byte[] mark = getHeaderData();
	if(Util.arraysEqual(Util.createCopy(mark, 0, 4), oldRarHeader)) {
	    throw new InvalidDataException("An older, unsupported RAR format was detected.");
	}
	else {
	    /* original RAR v2.0                                                  */
	    if(Util.arraysEqual(mark, rar2Header) ||
	       Util.arraysEqual(mark, uniqueHeader)) {
		    
	    }
	    else
		throw new InvalidDataException("Not a RAR file."); 
	}	    
    }
}
