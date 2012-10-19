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

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class NewFileHeader extends RARHeader {
    /*
     * struct NewFileHeader
     * size: 32 bytes
     *
     * BP  Size Description
     * 0   2    HeadCRC
     * 2   1    HeadType
     * 3   2    Flags
     * 5   2    HeadSize
     * 7   4    PackSize
     * 11  4    UnpSize
     * 15  1    HostOS
     * 16  4    FileCRC
     * 20  4    FileTime
     * 24  1    UnpVer
     * 25  1    Method
     * 26  2    NameSize
     * 28  4    FileAttr
     */

    // Possible values for the hostOS field in NewFileHeader, and possibly elsewhere...
    public static final int HOSTOS_MS_DOS = 0;
    public static final int HOSTOS_OS2 = 1;
    public static final int HOSTOS_WIN_32 = 2;
    public static final int HOSTOS_UNIX = 3;	
    public static final String[] hostOSStrings = { "MS-DOS", "OS/2", "Win32", "UNIX" };

    public static final int FM_NORMAL = 0x00;
    public static final int FM_RDONLY = 0x01;
    public static final int FM_HIDDEN = 0x02;
    public static final int FM_SYSTEM = 0x04;
    public static final int FM_LABEL = 0x08;
    public static final int FM_DIREC = 0x10;
    public static final int FM_ARCH = 0x20;

    public static final String PATH_SEPARATOR = "\\";
    
    private final byte[] packSize = new byte[4];
    private final byte[] unpSize = new byte[4];
    private final byte[] hostOS = new byte[1];
    private final byte[] fileCRC = new byte[4];
    private final byte[] fileTime = new byte[4];
    private final byte[] unpVer = new byte[1];
    private final byte[] method = new byte[1];
    private final byte[] nameSize = new byte[2];
    private final byte[] fileAttr = new byte[4];
    private final byte[] packSizeExtended;
    private final byte[] unpSizeExtended;
    private final byte[] filename;
    private final byte[] trailingData;
	
    public NewFileHeader(byte[] data, int offset) {
	super(data, offset);
	System.arraycopy(data, offset+7, packSize, 0, 4);
	System.arraycopy(data, offset+11, unpSize, 0, 4);
	System.arraycopy(data, offset+15, hostOS, 0, 1);
	System.arraycopy(data, offset+16, fileCRC, 0, 4);
	System.arraycopy(data, offset+20, fileTime, 0, 4);
	System.arraycopy(data, offset+24, unpVer, 0, 1);
	System.arraycopy(data, offset+25, method, 0, 1);
	System.arraycopy(data, offset+26, nameSize, 0, 2);
	System.arraycopy(data, offset+28, fileAttr, 0, 4);
	if(hasExtendedHeader()) {
	    packSizeExtended = new byte[4];
	    unpSizeExtended = new byte[4];
	    System.arraycopy(data, offset+32, packSizeExtended, 0, 4);
	    System.arraycopy(data, offset+36, unpSizeExtended, 0, 4);
	}
	else {
	    packSizeExtended = new byte[0];
	    unpSizeExtended = new byte[0];
	}
	final int extSize = packSizeExtended.length + unpSizeExtended.length;
	filename = new byte[getNameSize()];
	trailingData = new byte[getHeadSize()-(getStaticSize()+extSize+filename.length)];
	System.arraycopy(data, offset+32+extSize, filename, 0, filename.length);
	System.arraycopy(data, offset+32+extSize+filename.length, trailingData, 0, trailingData.length);
	validateData();
    }
	
    public static int getStaticSize() { return 32; }
    public int getSize() { return _getSize(); }
    private int _getSize() { return getHeadSize(); }
	
    protected void validateData() {
	//print(System.out, "");
	super.validateData();
	if(getHeadType() != FILE_HEAD)
	    throw new InvalidDataException("Incorrect head type! (headType=" + getHeadType() + ")");
	if(getHeadSize() < getStaticSize())
	    throw new InvalidDataException("Invalid size! (size=" + getHeadSize() + ")");
	if(getHostOSAsString() == null)
	    throw new InvalidDataException("Host OS value invalid.");
    }
    
    public long getPackSize() {
	long result = Util.readIntLE(packSize) & 0xFFFFFFFFL;
	if(packSizeExtended.length == 4)
	    result |= (Util.readIntLE(packSizeExtended) & 0xFFFFFFFFL) << 32;
	return result;
    }
    public long getUnpSize() {
	long result = Util.readIntLE(unpSize) & 0xFFFFFFFFL;
	if(unpSizeExtended.length == 4)
	    result |= (Util.readIntLE(unpSizeExtended) & 0xFFFFFFFFL) << 32;
	return result;
// 	if(unpSizeExtended.length == 4) {
// 	return Util.readIntLE(unpSize);
    }
    public byte getHostOS() { return Util.readByteLE(hostOS); }
    public int getFileCRC() { return Util.readIntLE(fileCRC); }
    public int getFileTime() { return Util.readIntLE(fileTime); }
    public byte getUnpVer() { return Util.readByteLE(unpVer); }
    public byte getMethod() { return Util.readByteLE(method); }
    public short getNameSize() { return Util.readShortLE(nameSize); }
    public int getFileAttr() { return Util.readIntLE(fileAttr); }
    public byte[] getFilename() { return Util.createCopy(filename); }
    public byte[] getTrailingData() { return Util.createCopy(trailingData); }

    public byte[] getHeaderData() {
	byte[] data = new byte[_getSize()];
	System.arraycopy(super.getHeaderData(), 0, data, 0, super.getSize());
	System.arraycopy(packSize, 0, data, 7, packSize.length);
	System.arraycopy(unpSize, 0, data, 11, unpSize.length);
	System.arraycopy(hostOS, 0, data, 15, hostOS.length);
	System.arraycopy(fileCRC, 0, data, 16, fileCRC.length);
	System.arraycopy(fileTime, 0, data, 20, fileTime.length);
	System.arraycopy(unpVer, 0, data, 24, unpVer.length);
	System.arraycopy(method, 0, data, 25, method.length);
	System.arraycopy(nameSize, 0, data, 26, nameSize.length);
	System.arraycopy(fileAttr, 0, data, 28, fileAttr.length);
	if(hasExtendedHeader()) {
	    System.arraycopy(packSizeExtended, 0, data, 32, 4);
	    System.arraycopy(unpSizeExtended, 0, data, 36, 4);
	}
	final int extSize = packSizeExtended.length + unpSizeExtended.length;
	System.arraycopy(filename, 0, data, 32+extSize, filename.length);
	System.arraycopy(trailingData, 0, data, 32+extSize+filename.length, trailingData.length);
	return data;
    }
    
    /**
     * true if the header is 40 bytes long, false if it is 32 bytes long
     * The 40 byte header exists to accommodate filesizes > 2^32
     */
    public boolean hasExtendedHeader() {
	return getFlag(8);
    }

    /**
     * true if this file is a continuation of a previous file (split archive)
     */
    public boolean hasIncomingData() {
	return getFlag(0);
    }
    
    /**
     * true if this file continues in another file (split archive)
     */
    public boolean hasOutgoingData() {
	return getFlag(1);
    }
    
    public Date getFileTimeAsDate() {
	int data = getFileTime();
	int year = 1980 + ((data >> (4+5+5+6+5)) & 0x7F);
	int month = ((data >> (5+5+6+5)) & 0xF)-1;
	int day = (data >> (5+6+5)) & 0x1F;
	int hour = (data >> (6+5)) & 0x1F;
	int minute = (data >> (5)) & 0x3F;
	int doublesecond = data & 0x1F; // the archive format only supports 2-second precision (probably due to simpler design)
	
// 	System.out.println("year = " + year);
// 	System.out.println("month = " + month);
// 	System.out.println("day = " + day);
// 	System.out.println("hour = " + hour);
// 	System.out.println("minute = " + minute);
// 	System.out.println("doublesecond = " + doublesecond);
	
	Calendar c = Calendar.getInstance();
	c.setTime(new Date(0));
	c.set(Calendar.YEAR, year);
	c.set(Calendar.MONTH, month);
	c.set(Calendar.DAY_OF_MONTH, day);
	c.set(Calendar.HOUR, hour);
	c.set(Calendar.MINUTE, minute);
	c.set(Calendar.SECOND, doublesecond*2);
	return c.getTime();
    }

    public FileAttributes getFileAttributesStructured() {
	byte hostOS = getHostOS();
	if(hostOS == HOSTOS_WIN_32)
	    return new Win32FileAttributes(getFileAttr());
	else if(hostOS == HOSTOS_UNIX)
	    return new UNIXFileAttributes(getFileAttr());
	else if(hostOS == HOSTOS_MS_DOS || hostOS == HOSTOS_OS2)
	    return new Win32FileAttributes(getFileAttr()); // Win, DOS and OS/2 seems to have a lot in common, but I don't really know what the file attributes will look like under DOS and OS/2. Hopefully something like the Win32-attributes (:
	else
	    throw new RuntimeException("Unknown OS type!");
    }
    
//     public boolean isFile() {
// 	return getFlag(getFileAttr(), 13);
//     }
    
    public String getHostOSAsString() {
	return hostOSToString(getHostOS());
    }

    public String getUnpackVersionAsString() {
	byte unpVer = getUnpVer();
	return (unpVer/10) + "." + (unpVer%10);
    }
	
    public String getFilenameAsString() {
	String result = getUnicodeFilenameAsString();
	if(result == null)
	    result = getLegacyFilenameAsString();
	return result;
    }
    public String getLegacyFilenameAsString() {
	/* After testing, it seems that one of the old MS-DOS charsets
	   are used to store the file name in this structure. If
	   different charsets are supported, I don't know how to detect
	   which charset will be used yet. Code Page 850 is used for now. */
	try {
	    return new String(filename, /*"IBM437"*/"iso-8859-1").split("\0")[0];
	} catch(UnsupportedEncodingException uee) {
	    throw new RuntimeException(uee);
	}
    }

    public String getUnicodeFilenameAsString() {
	/* To cope with characters outside the old IBM charset originally
	   used for filenames, RAR embeds extra data after the area
	   containing the IBM charset filename. The old style filename is
	   terminated by null, and after that follows the characters that
	   could not be encoded properly with the IBM charset. */
	boolean debug = false;
	if(debug) System.out.println("getExtendedCharsetFilenameAsString();");
	char[] legacyFilename = getLegacyFilenameAsString().toCharArray();
	char[] unicodeFilename = new char[legacyFilename.length];
	int unicodeFilenamePointer = 0;
	//LinkedList<Character> codeUnitList = new LinkedList<Character>();
	
	// Find the index at which the extended filename characters begin (first occurrence of 0x00).
	int zeroIndex = 0;
	for(; zeroIndex < filename.length; ++zeroIndex)
	    if(filename[zeroIndex] == 0)
		break;
	
	int codeUnitsRead = 0;;

	int extendedDataPointer = zeroIndex+1;
	if(extendedDataPointer+2 >= filename.length) // Must have compressedLSB (1 bytes) and at least one byte
	    return null;
	byte compressedLSB = Util.readByteLE(filename, extendedDataPointer++);
	
	mainLoop:
	while(extendedDataPointer < filename.length) {
	    if(debug) System.out.println(" Reading 4 bytes... (found " + codeUnitsRead + 
					 " code points, extendedDataPointer=" + extendedDataPointer + ")");
	    byte groupDescriptor = Util.readByteLE(filename, extendedDataPointer++);
	    for(int i = 0; (i < 4) && (extendedDataPointer < filename.length); ++i) {
		byte current = (byte)((groupDescriptor >>> ((3-i)*2)) & 0x3); // 0b11
		if(current == 0x0) { // 0b00
		    //if(extendedDataPointer >= filename.length) break;
		    char codeUnit = (char)(Util.readByteLE(filename, extendedDataPointer++) & 0xFF);
		    if(debug) System.out.println("  Encountered 8-bit character: 0x" + Util.toHexStringBE(codeUnit));
		    //codeUnitList.add(codeUnit);
		    unicodeFilename[unicodeFilenamePointer++] = codeUnit;
		    ++codeUnitsRead;
		}
		else if(current == 0x1) {
		    //if(extendedDataPointer >= filename.length) break;
		    char codeUnit = (char)(((compressedLSB & 0xFF) << 8) | 
					   (Util.readByteLE(filename, extendedDataPointer++) & 0xFF));
		    if(debug) System.out.println("  Encountered compressed 16-bit character: 0x" +
						 Util.toHexStringBE(codeUnit));
		    //codeUnitList.add(codeUnit);
		    unicodeFilename[unicodeFilenamePointer++] = codeUnit;
		    ++codeUnitsRead;
		}
		else if(current == 0x2) { // 0b10
		    //if(extendedDataPointer+1 >= filename.length) break;
		    char codeUnit = (char)Util.readShortLE(filename, extendedDataPointer);
		    extendedDataPointer += 2;
		    if(debug) System.out.println("  Encountered 16-bit character: 0x" + Util.toHexStringBE(codeUnit));
		    //codeUnitList.add(codeUnit);
		    unicodeFilename[unicodeFilenamePointer++] = codeUnit;
		    ++codeUnitsRead;
		}
		else if(current == 0x3) {
		    //if(extendedDataPointer >= filename.length) break;
		    byte indicator = Util.readByteLE(filename, extendedDataPointer++);
		    
		    /* Detta är bullshit. Det är INTE en indikator utan ett "skip"-kommando.
		     * Ex. skip(0x02) => Spola förbi 4 characters dvs. ".txt" (nästan alla test*+.rar)
		     *     skip(0x05) => Spola förbi 7 characters ex. "bok.txt" (ctest6.rar)
		     *     skip(0x01) => Spola förbi 3 characters ex. "Hej" (ctest16.rar)
		     *     skip(0x03) => Spola förbi 5 characters ex. "laban" (ctest16.rar)
		     * Specialfall:
		     * - Man kan som minst spola förbi 2 characters (skip(0x00)). Om strängen inleds med
		     *   en ASCII-character med Unicode-char direkt följande så kodas ASCII-charactern ner
		     *   som Unicode.
		     * - På samma sätt kan man inte spola förbi 131 characters, utan är tvungen att koda
		     *   skip(0x7F)... eller nåt. iaf skitsamma. orka.
		     */
		    
		    if((indicator & 0xFF) > 0x7F)
			System.out.println("  Encountered unexpected skip value! 0x" + Util.toHexStringBE(indicator));
		    if(debug) System.out.println("  Skipping " + (indicator+2) + " bytes.");
		    
		    System.arraycopy(legacyFilename, unicodeFilenamePointer, unicodeFilename, unicodeFilenamePointer, (indicator+2));
		    unicodeFilenamePointer += (indicator+2);
		}
	    }
	}
	if(unicodeFilenamePointer != legacyFilename.length)
	    if(debug) System.out.println(" ERROR! Did not read enough characters. Read " + unicodeFilenamePointer + " characters while legacyFilename.length==" + legacyFilename.length + ".");
	else
	    if(debug) System.out.println(" Read " + unicodeFilenamePointer + " characters.");
	return new String(unicodeFilename);
    }
	
    public byte[] getTrailingFilenameData() {
	for(int i = 0; i < filename.length; ++i) {
	    if(filename[i] == 0)
		return Util.createCopy(filename, i, filename.length-i);
	}
	return new byte[0];
    }
	
    public static String hostOSToString(byte hostOS) {
	if(hostOS >= 0 && hostOS <= hostOSStrings.length)
	    return hostOSStrings[hostOS];
	else
	    return null;
    }

    /* // An attempt at porting the CRC-function from unrarlib to Java. Seems to work poorly.
       private static class MyCRC32 {
       private final int[] CRCTab = new int[256];
       private int currentCRC = 0xFFFFFFFF;
       public MyCRC32() {
       short I, J;
       int C;
       for(I=0;(I&0xFFFF)<256;I++) {
       for(C=I,J=0;J<8;J++)
       C=((C & 1) != 0)? (C>>1)^0xEDB88320 : (C>>1);
       CRCTab[I&0xFFFF]=C;
       }
       }
	    
       public void update(byte[] data) {
       currentCRC = CalcCRC32(currentCRC, data);
       }

       public int getValue() {
       return currentCRC;
       }
	    
       private int CalcCRC32(int StartCRC, byte[] Addr) {
       short I;
       for(I=0; I<Addr.length; I++)
       StartCRC = CRCTab[((byte)StartCRC ^ Addr[I])&0xFF] ^ (StartCRC >> 8);
       return(StartCRC);
       }
       }
    */

	
    protected void printFields(PrintStream ps, String prefix) {
	super.printFields(ps, prefix);
	ps.println(prefix + " PackSize: " + getPackSize() + " bytes");
	ps.println(prefix + " UnpSize: " + getUnpSize() + " bytes");
	ps.println(prefix + " HostOS: \"" + getHostOSAsString() + "\"");
	ps.println(prefix + " FileCRC: 0x" + Util.toHexStringBE(getFileCRC()));
	ps.println(prefix + " FileTime: " + getFileTimeAsDate() + " (0x" + Util.toHexStringBE(getFileTime()) + ")");
	ps.println(prefix + " UnpVer: \"" + getUnpackVersionAsString() + "\"");
	ps.println(prefix + " Method: 0x" + Util.toHexStringBE(getMethod()));
	ps.println(prefix + " NameSize: " + getNameSize() + " bytes");
	ps.println(prefix + " FileAttr: 0x" + Util.toHexStringBE(getFileAttr()));
	ps.println(prefix + " Structured file attributes:");
	getFileAttributesStructured().print(ps, prefix + "  ");
	ps.println(prefix + " Filename: \"" + getFilenameAsString() + "\"");
	byte[] trailingFilenameData = getTrailingFilenameData();
	if(trailingFilenameData.length > 0)
	    ps.println(prefix + "  (Trailing filename data: 0x" + Util.byteArrayToHexString(trailingFilenameData) + ")");
	ps.println(prefix + " Trailing data: 0x" + Util.byteArrayToHexString(getTrailingData()));
    }

    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "NewFileHeader: ");
	printFields(ps, prefix);
    }
}
