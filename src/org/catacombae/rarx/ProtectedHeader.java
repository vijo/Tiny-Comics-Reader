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

public class ProtectedHeader extends RARHeader {
    private final byte[] dataSize = new byte[4];
    private final byte[] unknownData;
    public ProtectedHeader(byte[] data, int offset) {
	super(data, offset);
	System.arraycopy(data, super.getSize(), dataSize, 0, 4);
	unknownData = new byte[getHeadSize()-(super.getSize()+4)];
	System.arraycopy(data, super.getSize()+4, unknownData, 0, unknownData.length);
	validateData();
    }

    public int getSize() {
	return _getSize();
    }

    private int _getSize() {
	return super.getSize()+dataSize.length+unknownData.length;
    }

    public void validateData() {
	super.validateData();
	if(getHeadType() != PROTECT_HEAD)
	    throw new InvalidDataException("Incorrect head type! (headType=" + getHeadType() + ")");	
    }

    public byte[] getHeaderData() {
	byte[] outData = new byte[_getSize()];
	byte[] superData = super.getHeaderData();
	System.arraycopy(superData, 0, outData, 0, superData.length);
	System.arraycopy(dataSize, 0, outData, superData.length, dataSize.length);
	System.arraycopy(unknownData, 0, outData, superData.length+dataSize.length, unknownData.length);
	return outData;
    }
    
    public long getDataSize() {
	return Util.readIntLE(dataSize, 0) & 0xFFFFFFFFL;
    }
    
    public void print(PrintStream ps, String prefix) {
	System.out.println("Protected header:");
	printFields(ps, prefix);
    }    
}
