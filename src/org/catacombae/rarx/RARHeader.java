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
import java.util.zip.CRC32;

public abstract class RARHeader {
    /*
     * Common fields for all RAR headers
     * size: 7 bytes
     *
     * BP  Size Description
     * 0   2    HeadCRC
     * 2   1    HeadType
     * 3   2    Flags
     * 5   2    HeadSize
     */
    // Possible values for the fields headType in various classes
    //public static final int ALL_HEAD = 0;
    public static final int MARK_HEAD = 0x72; // 0x69 for UniquE! ones..? However, this field isn't really needed.
    public static final int MAIN_HEAD = 0x73;
    public static final int FILE_HEAD = 0x74;
    public static final int COMM_HEAD = 0x75;
    public static final int AV_HEAD = 0x76;
    public static final int SUB_HEAD = 0x77;
    public static final int PROTECT_HEAD = 0x78;
    public static final int COMMENT_HEAD = 0x7a;
    public static final int EOF_HEAD = 0x7b;
    
    /* The versioning seems very simple. version 2.0 is represented
       by the value 20 (0x14), 2.6 by the value 26 (0x1a) etc. */
    public static final int UNPACK_VERSION_2_0 = 0x14;
    public static final int UNPACK_VERSION_2_6 = 0x1a; // efterlyst-bfc.rar
    public static final int UNPACK_VERSION_2_9 = 0x1d;

    public static final int METHOD_UNCOMPRESSED = 0x30; // Observerad i 2.0, 2.9
    public static final int METHOD_COMPRESSION_1 = 0x35; // Observerad i 2.0, 2.9
    public static final int METHOD_COMPRESSION_2 = 0x33; // Observerad i 2.0 (flera gånger, men ovanligt), 2.9

    /* Hypotes: de två minst signifikanta bitarna i Flags indikerar om en fil
       fortsätter på nytt medium. Om bit 0 är satt är den aktuella filen en
       fortsättning på en tidigare fil. Om bit 1 är satt fortsätter den
       aktuella filen på ytterligare nya media.
       Hypotes bekräftad. */


    private final byte[] headCRC = new byte[2];
    private final byte[] headType = new byte[1];
    private final byte[] flags = new byte[2];
    private final byte[] headSize = new byte[2];
    private final CRC32 crc = new CRC32();
    
    public RARHeader(byte[] data, int offset) {
	System.arraycopy(data, offset+0, headCRC, 0, 2);
	System.arraycopy(data, offset+2, headType, 0, 1);
	System.arraycopy(data, offset+3, flags, 0, 2);
	System.arraycopy(data, offset+5, headSize, 0, 2);
    }

    public int getSize() { return _getSize(); }
    private int _getSize() { return 7; }
    
    protected void validateData() {
	if(calculateHeadCRC() != getHeadCRC())
	    throw new InvalidDataException("Incorrect header CRC!");
    }

    public short getHeadCRC() { return Util.readShortLE(headCRC); }
    public byte getHeadType() { return Util.readByteLE(headType); }
    public short getFlags() { return Util.readShortLE(flags); }
    public short getHeadSize() { return Util.readShortLE(headSize); }

    public boolean getFlag(int bitNumber) {
	return Util.getBit(getFlags(), bitNumber);//((getFlags() >> byteNumber) & 0x1) == 0x1;
    }

    
    public byte[] getHeaderData() {
	byte[] data = new byte[_getSize()];
	System.arraycopy(headCRC, 0, data, 0, headCRC.length);
	System.arraycopy(headType, 0, data, 2, headType.length);
	System.arraycopy(flags, 0, data, 3, flags.length);
	System.arraycopy(headSize, 0, data, 5, headSize.length);
	return data;
    }
    
    public short calculateHeadCRC() {
	crc.reset();
	crc.update(getHeaderData(), 2, getSize()-2);
	return (short)(crc.getValue());
    }
    protected void printFields(PrintStream ps, String prefix) {
	ps.println(prefix + " HeadCRC: " + Util.toHexStringBE(getHeadCRC()));
	if(calculateHeadCRC() != getHeadCRC())
	    ps.println(prefix + "  CHECKSUMS NOT MATCHING! (0x" + 
		       Util.toHexStringBE(getHeadCRC()) + " != 0x" + 
		       Util.toHexStringBE(calculateHeadCRC()) + "))");
	ps.println(prefix + " HeadType: " + Util.toHexStringBE(getHeadType()));
	ps.println(prefix + " Flags: " + Util.toHexStringBE(getFlags()));
	ps.println(prefix + " HeadSize: " + getHeadSize() + " bytes");
    }
}
