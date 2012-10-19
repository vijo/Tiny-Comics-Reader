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

public class UnknownHeader extends RARHeader {
    private final byte[] data;
    
    public UnknownHeader(byte[] inData, int offset) {
	super(inData, offset);
	data = new byte[getHeadSize()-super.getSize()];
	System.arraycopy(inData, super.getSize(), data, 0, data.length);
	super.validateData();
    }
    
    public int getSize() {
	return _getSize();
    }
    private int _getSize() {
	return super.getSize()+data.length;
    }
    
    public byte[] getHeaderData() {
	byte[] outData = new byte[_getSize()];
	byte[] superData = super.getHeaderData();
	System.arraycopy(superData, 0, outData, 0, superData.length);
	System.arraycopy(data, 0, outData, superData.length, data.length);
	return outData;
    }

    public void print(PrintStream ps, String prefix) {
	System.out.println("Unknown header:");
	printFields(ps, prefix);
    }
}
