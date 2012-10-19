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

import java.io.PrintStream;

public class CommentHeader extends NewFileHeader {
    // This file header type is observed under UnpVer 0x1d (RAR 2.9)
    // This file header type is actually an archive comment!
    /* This type of header has a structure extactly like the NewFileHeader, but
       with a different head type (0x7a). The filename is probably always "CMT"
       (for "Comment"), and the data found when extracting it is the archive
       comment. */

    public CommentHeader(byte[] data, int offset) {
	super(data, offset);
	//super.super.validateData();
    }

    protected void validateData() {
	//print(System.out, "");
	//super.super.validateData(); //arvsproblem...
	if(getHeadType() != COMMENT_HEAD)
	    throw new InvalidDataException("Incorrect head type! (headType=" + getHeadType() + ")");
	if(getHeadSize() < getStaticSize())
	    throw new InvalidDataException("Invalid size! (size=" + getHeadSize() + ")");
	if(getHostOSAsString() == null)
	    throw new InvalidDataException("Host OS value invalid.");
	
    }

    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "CommentHeader: ");
	printFields(ps, prefix);
    }
}
