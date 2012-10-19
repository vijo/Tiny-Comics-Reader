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

public class NewMainArchiveHeader extends RARHeader {
    /*
     * struct NewMainArchiveHeader
     * size: 13 bytes
     *
     * BP  Size Description
     * 0   2    HeadCRC
     * 2   1    HeadType
     * 3   2    Flags
     * 5   2    HeadSize
     * 7   2    Reserved
     * 9   4    Reserved1
     */
	
    private final byte[] reserved = new byte[2];
    private final byte[] reserved1 = new byte[4];
	
    public NewMainArchiveHeader(byte[] data, int offset) {
	super(data, offset);
	System.arraycopy(data, offset+7, reserved, 0, 2);
	System.arraycopy(data, offset+9, reserved1, 0, 4);
	validateData();
    }
	
    public int getSize() { return _getSize(); }
    private int _getSize() { return getHeadSize();/*13;*/ }
	
    protected void validateData() {
	super.validateData();
	if(getHeadType() != MAIN_HEAD)
	    throw new InvalidDataException("Incorrect head type! (headType=" + getHeadType() + ")");
// 	if(getHeadSize() != getSize())
// 	    throw new InvalidDataException("Wrong size! (size=" + getHeadSize() + ")");
    }

    public short getReserved() { return Util.readShortLE(reserved); }
    public int getReserved1() { return Util.readIntLE(reserved1); }
	
    public byte[] getHeaderData() {
	byte[] data = new byte[_getSize()];
	System.arraycopy(super.getHeaderData(), 0, data, 0, super.getSize());	
	System.arraycopy(reserved, 0, data, 7, 2);
	System.arraycopy(reserved1, 0, data, 9, 4);
	return data;
    }
//     public short calculateHeadCRC() {
// 	CRC32 crc = new CRC32();
// 	//MyCRC32 crc = new MyCRC32();
// 	// 	    crc.update(headCRC);
// 	crc.update(headType);
// 	crc.update(flags);
// 	crc.update(headSize);
// 	crc.update(reserved);
// 	crc.update(reserved1);
// 	//crc.(trailingData);
// 	return (short)(crc.getValue());
//     }
	
    protected void printFields(PrintStream ps, String prefix) {
	super.printFields(ps, prefix);
	ps.println(prefix + " Reserved: " + Util.toHexStringBE(getReserved()));
	ps.println(prefix + " Reserved1: " + Util.toHexStringBE(getReserved1()));
    }
    

    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "NewMainArchiveHeader: ");
	printFields(ps, prefix);
    }
}
