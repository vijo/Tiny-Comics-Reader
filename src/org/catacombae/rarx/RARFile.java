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

import java.util.Vector;
import java.io.*;
import java.util.zip.CRC32;

public class RARFile {
    public static final int SIZEOF_MARKHEAD = 7;
    public static final int SIZEOF_OLDMHD = 7;
    public static final int SIZEOF_NEWMHD = 13;
    public static final int SIZEOF_OLDLHD = 21;
    public static final int SIZEOF_NEWLHD = 32;
    public static final int SIZEOF_SHORTBLOCKHEAD = 7;
    public static final int SIZEOF_LONGBLOCKHEAD = 11;
    public static final int SIZEOF_COMMHEAD = 13;
    public static final int SIZEOF_PROTECTHEAD = 26;
    
    private static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws IOException {
	File fileListFile = new File(args[0]);
	BufferedReader fileListIn = new BufferedReader(new InputStreamReader(new FileInputStream(fileListFile), "UTF-8"));
	String currentRar = fileListIn.readLine();
	while(currentRar != null) {
	    if(true) {
		System.out.println("Processing file " + currentRar + "...");
		try {
		    RARFileEntry[] entries = ListFilesInArchive.listFiles(new File(currentRar));
		    for(RARFileEntry entry : entries) {
			NewFileHeader nfh = entry.getHeader(0);
			if(nfh instanceof CommentHeader)
			    continue;
			//if(nfh.hasOutgoingData())
			System.out.print((nfh.hasIncomingData()?"<--":"   ") + nfh.getFilenameAsString() + (nfh.hasOutgoingData()?"-->":""));
			if(nfh.getFileAttributesStructured().isDirectory())
			    System.out.print(" (Dir)");
			else
			    System.out.print(" (File)");
			System.out.println();
		    }
		} catch(InvalidDataException ide) {
		    System.out.println("Invalid RAR-file. Message: " + ide.getMessage());
		    System.out.println("File: \"" + currentRar + "\"");
		}
	    }
	    else {
		RandomAccessFile raf = new RandomAccessFile(currentRar, "r");
		try {
		    readMarkHeader(raf);
		    NewMainArchiveHeader nmah = readNewMainArchiveHeader(raf);
		    NewFileHeader nfh = readNewFileHeader(raf);
		} catch(InvalidDataException e) {
		    System.out.println("Invalid RAR-file. Message: " + e.getMessage());
		    System.out.println("File: \"" + currentRar + "\"");
		    //System.exit(1);
		}
	    }
	    System.out.print("Press enter to continue...");
	    stdin.readLine();
	    System.out.println();
	    currentRar = fileListIn.readLine();
	}
    }
    public static void main2(String[] args) throws IOException {
	RandomAccessFile raf = new RandomAccessFile(args[0], "r");
	try {
	    readMarkHeader(raf);
	} catch(InvalidDataException e) {
	    System.out.println("Invalid RAR-file. Message: " + e.getMessage());
	    System.exit(1);
	}
	
	NewMainArchiveHeader nmah = readNewMainArchiveHeader(raf);
	nmah.print(System.out, "");
	NewFileHeader nfh = readNewFileHeader(raf);
	nfh.print(System.out, "");
    }
    
    public static Vector list(String path, boolean fullPath) throws IOException {
	RandomAccessFile raf = new RandomAccessFile(path, "r");
	raf.seek(7);
	NewMainArchiveHeader nmah = readNewMainArchiveHeader(raf);
	return null;
    }

    public static boolean isRAR(RandomAccessFile raf) throws IOException {
	long oldFilePointer = raf.getFilePointer();
	raf.seek(0);
	byte[] rarHeader = new byte[7];
	raf.read(rarHeader);
	/* Old archive => error */
	if(rarHeader[0]==0x52 && rarHeader[1]==0x45 &&
	   rarHeader[2]==0x7e && rarHeader[3]==0x5e) {
	    //debug_log("Attention: format as OLD detected! Can't handle archive!");
	}
	else {
	    /* original RAR v2.0 */
	    if((rarHeader[0]==0x52 && rarHeader[1]==0x61 && /* original  */
		rarHeader[2]==0x72 && rarHeader[3]==0x21 && /* RAR header*/
		rarHeader[4]==0x1a && rarHeader[5]==0x07 &&
		rarHeader[6]==0x00) ||
	       /* "UniquE!" - header */
	       (rarHeader[0]=='U' && rarHeader[1]=='n' &&   /* "UniquE!" */
		rarHeader[2]=='i' && rarHeader[3]=='q' &&   /* header    */
		rarHeader[4]=='u' && rarHeader[5]=='E' &&
		rarHeader[6]=='!')) {
		if(readNewMainArchiveHeader(raf) != null)
		    return true;
	    } else {
		// Skriv ut nåt felmedd...
	    }
	}
	return false;
    }
    
    public static MarkHeader readMarkHeader(RandomAccessFile raf) throws IOException {
	byte[] header = new byte[7];
	raf.read(header);
	MarkHeader nfh = new MarkHeader(header, 0);
	return nfh;
    }
    public static NewMainArchiveHeader readNewMainArchiveHeader(RandomAccessFile raf) throws IOException {
	byte[] mainHeader = new byte[13];
	raf.read(mainHeader);
	NewMainArchiveHeader nmah = new NewMainArchiveHeader(mainHeader, 0);
	return nmah;
    }

    public static NewFileHeader readNewFileHeader(RandomAccessFile raf) throws IOException {
	byte[] header = new byte[32];
	raf.read(header);
	int headerSize = Util.readShortLE(header, 5);
	byte[] fullHeader = new byte[headerSize];
	System.arraycopy(header, 0, fullHeader, 0, header.length);
	raf.read(fullHeader, header.length, fullHeader.length-header.length);
	NewFileHeader nfh = new NewFileHeader(fullHeader, 0);
	byte[] reconstruction = nfh.getHeaderData();
	if(!Util.arraysEqual(fullHeader, reconstruction))
	    System.out.println("ERROR: Data after reconstruction has changed!");
	return nfh;
    }

    public static RARHeader readHeader(RandomAccessFile raf) throws IOException {
	byte[] header = new byte[7];
	if(raf.read(header) == -1)
	    return null;
	int headerSize = Util.readShortLE(header, 5);
	byte headerType = Util.readByteLE(header, 2);
	try {
	    byte[] fullHeader = new byte[headerSize];
	    System.arraycopy(header, 0, fullHeader, 0, header.length);
	    raf.read(fullHeader, header.length, fullHeader.length-header.length);
	    //System.out.println("Reading... headerSize=" + headerSize + " headerType=" + headerType);
	    if(headerType == RARHeader.FILE_HEAD) {
		NewFileHeader nfh = new NewFileHeader(fullHeader, 0);
		raf.seek(raf.getFilePointer()+nfh.getPackSize());
		return nfh;
	    }
	    else if(headerType == RARHeader.MAIN_HEAD)
		return new NewMainArchiveHeader(fullHeader, 0);
	    else if(headerType == RARHeader.COMMENT_HEAD) {
		CommentHeader ch = new CommentHeader(fullHeader, 0);
		raf.seek(raf.getFilePointer()+ch.getPackSize());
		return ch;
	    }
	    else if(headerType == RARHeader.PROTECT_HEAD) {
		ProtectedHeader ph = new ProtectedHeader(fullHeader, 0);
		raf.seek(raf.getFilePointer()+ph.getDataSize());
		return ph;
	    }
	    else if(headerType == RARHeader.EOF_HEAD)
		return null;
	    else {
		System.out.println("UNRECOGNIZED HEADER TYPE: 0x" + Util.toHexStringBE(headerType) + " for header at 0x" + Util.toHexStringBE(raf.getFilePointer()-headerSize)); 
		UnknownHeader uh = new UnknownHeader(fullHeader, 0);
		uh.print(System.out, "");
		return uh;
	    }
	} catch(ArrayIndexOutOfBoundsException aioobe) {
	    aioobe.printStackTrace();
	    System.err.println("headerSize=" + headerSize);
	    System.err.println("headerType=" + headerType);
	    System.err.println("filePointer: " + raf.getFilePointer());
	    return null;
	}
    }

    public static long findEntry(String entryName, RandomAccessFile rarFile) throws IOException {
	readMarkHeader(rarFile);
	RARHeader currentHeader = readHeader(rarFile);
	while(currentHeader != null) {
	    if(currentHeader instanceof NewFileHeader && 
	       !(currentHeader instanceof CommentHeader)) {
		NewFileHeader nfh = (NewFileHeader)currentHeader;
		if(nfh.getFilenameAsString().equals(entryName)) {
		    return rarFile.getFilePointer() - (nfh.getHeadSize()+nfh.getPackSize());
		}
	    }
	    currentHeader = readHeader(rarFile);
	}
	return -1;
    }

    // Utility methods
    
//     public static int readIntLE(byte[] data) {
// 	return readIntLE(data, 0);
//     }
//     public static int readIntLE(byte[] data, int offset) {
// 	return ((data[offset+3] & 0xFF) << 24 |
// 		(data[offset+2] & 0xFF) << 16 |
// 		(data[offset+1] & 0xFF) << 8 |
// 		(data[offset+0] & 0xFF) << 0);
//     }
//     public static short readShortLE(byte[] data) {
// 	return readShortLE(data, 0);
//     }
//     public static short readShortLE(byte[] data, int offset) {
// 	return (short) ((data[offset+1] & 0xFF) << 8 |
// 			(data[offset+0] & 0xFF) << 0);
//     }
//     public static byte readByteLE(byte[] data) {
// 	return readByteLE(data, 0);
//     }
//     public static byte readByteLE(byte[] data, int offset) {
// 	return data[offset];
//     }
}
