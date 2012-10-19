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

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.zip.CRC32;

public class ListArchiveContents {
    
    /*
     * Konceptuellt: Vi har en buffer |E4F91...|A81984B....|
     *                                ^--------^ <- 32 (0x20) bytes, 
     */

    //public static final byte[] entrySignature = { 0x7C, 0x30, 0x14, 0x30 };
    public static final String BACKSPACE79 = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
    public static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    public static final short ENTRY_TYPE_DIR = 0x0010;
    public static final short ENTRY_TYPE_FILE = 0x0020;
    public static void main(String[] args) throws IOException {
	boolean deleteOnError = true; // Change this to be decided by a command line parameter.
	RandomAccessFile raf = new RandomAccessFile(args[0], "r");
	File outDir = null;
	if(args.length > 1)
	    outDir = new File(args[1]);
	byte[] buffer = new byte[4096];
	byte[] pre = new byte[0x16];
	byte[] signature = new byte[0x4];
	byte[] post = new byte[0x6];
	byte[] postString = new byte[0x5];
	byte[] staticEntryData = new byte[pre.length+signature.length+
					  post.length+postString.length];
	for(int i = 0; i < staticEntryData.length; ++i)
	    staticEntryData[i] = 0;
	ArrayList<byte[]> savedEntries = new ArrayList<byte[]>();
	ArrayList<byte[]> dirEntries = new ArrayList<byte[]>();
	ArrayList<byte[]> fileEntries = new ArrayList<byte[]>();
	LinkedList<String> extractedFiles = new LinkedList<String>();
	LinkedList<String> badFiles = new LinkedList<String>();
	LinkedList<String> createErrorFiles = new LinkedList<String>();
	LinkedList<String> badFilesizeFiles = new LinkedList<String>();
	int signatureCount = 0;
	int readAhead = pre.length+4+post.length;
	int bytesRead = raf.read(buffer);//, readAhead, buffer.length-readAhead);
	long totalBytesRead = bytesRead;
	int lastReadPos = bytesRead;
	//bytesRead += readAhead;
	while(raf.getFilePointer() < raf.length()) {
	    for(int i = pre.length; i < lastReadPos-(signature.length+post.length); ++i) {
		if(buffer[i+(0x18-0x16)] == 0x14 && 
		   //buffer[i+(0x19-0x16)] == 0x30 && // This one is not really common for all rars.
		   //buffer[i+(0x16-0x16)] == 0x7c && buffer[i+(0x17-0x16)] == 0x30
		   buffer[i+(0x1b-0x16)] == 0 &&
		   (buffer[i+(0x1c-0x16)] == 0x20 || buffer[i+(0x1c-0x16)] == 0x10) &&
		   buffer[i+(0x1d-0x16)] == 0 &&
		   buffer[i+(0x1e-0x16)] == 0 &&
		   buffer[i+(0x1f-0x16)] == 0 //&&
		   //buffer[i+(0x20-0x16)] == 0
		   ) {
// 		    System.out.println("buffer[i+(0x1b-0x16)] == " + buffer[i+(0x1b-0x16)]);
// 		    System.out.println("buffer[i+(0x1c-0x16)] == " + buffer[i+(0x1c-0x16)]);
// 		    System.out.println("buffer[i+(0x1d-0x16)] == " + buffer[i+(0x1d-0x16)]);
// 		    System.out.println("buffer[i+(0x1e-0x16)] == " + buffer[i+(0x1e-0x16)]);
// 		    System.out.println("buffer[i+(0x1f-0x16)] == " + buffer[i+(0x1f-0x16)]);
		    ++signatureCount;
		    System.out.println("File found at position " + ((raf.getFilePointer()-lastReadPos)+i));
		    int fileSize = Util.readIntLE(buffer, i-0xf);
		    System.out.println("  Size: " + fileSize + " bytes (0x" + Util.toHexStringBE(fileSize) + ")");
		    int crc = Util.readIntLE(buffer, i-6);
		    System.out.println("  CRC32: 0x" + Util.toHexStringBE(crc));
		    int stringSize = Util.readShortLE(buffer, i+4) & 0xFFFF;
		    System.out.println("  String size: " + stringSize);
		    short fileType = Util.readShortLE(buffer, i+6);
		    System.out.println("  Some type: 0x" + Util.toHexStringBE(fileType));
		    int difference = i+signature.length+post.length+stringSize+postString.length-lastReadPos;
		    
		    if(difference > 0) {
			System.out.println("Beginning of string: " + new String(buffer, i+signature.length+post.length, stringSize-(difference-postString.length), "US-ASCII"));
// 			System.out.println("  i=" + i + " post.length=" + post.length + " bytesRead=" + bytesRead);
// 			System.out.println("  String size: " + Util.toHexStringBE(stringSize) + " Difference: " + Util.toHexStringBE(difference));
			byte[] diffArray = new byte[difference];
			int tempBytesRead = raf.read(diffArray);
// 			System.out.println("  Contents of diffarray: " + new String(diffArray, "US-ASCII"));
			if(tempBytesRead != diffArray.length)
			    throw new RuntimeException("Read error."); // Orka..
			totalBytesRead += tempBytesRead;
			
			System.arraycopy(buffer, difference, buffer, 0, lastReadPos-difference);
			System.arraycopy(diffArray, 0, buffer, lastReadPos-difference, diffArray.length);
			i -= difference;
// 			System.out.println("  After stringshift: " + new String(buffer, i+4+post.length, stringSize-difference, "US-ASCII"));
// 			System.out.println("  " + new String(buffer, i+4+post.length, stringSize, "US-ASCII"));
		    }

		    // Save the data for future analysis
		    byte[] data = new byte[staticEntryData.length];
		    System.arraycopy(buffer, i-pre.length, data, 0, pre.length+signature.length+post.length);
		    System.arraycopy(buffer, i+signature.length+post.length+stringSize, data, pre.length+signature.length+post.length, postString.length);
		    savedEntries.add(data);
		    
		    try {
			/*
			javax.swing.JFrame jf = new javax.swing.JFrame();
			javax.swing.JTextArea tf = new javax.swing.JTextArea(80, 50);
			tf.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			jf.add(new javax.swing.JScrollPane(tf));
			jf.pack();
			jf.setVisible(true);
			for(String charset : java.nio.charset.Charset.availableCharsets().keySet())
			    tf.append("Name: \"" + new String(buffer, i+4+post.length, stringSize, charset) + "\" in charset: " + charset + "\n");
			if(true)
			    return;
			*/
			
			String fullFilename = new String(buffer, i+4+post.length, stringSize, "IBM437");
			String[] filenameComponents = fullFilename.split("\0");
			String filename = filenameComponents[0];
			System.out.println("  Name: \"" + filename + "\"");
			//stdin.readLine();
			System.out.println("  In hex: 0x" + Util.byteArrayToHexString(buffer, i+4+post.length, stringSize));
			if(fileType == ENTRY_TYPE_DIR) {
			    System.err.println("Assuming to be dir: " + filename);
			    dirEntries.add(data);
			}
// 			else if(fileSize == 0 || crc == 0 || filename.split("\\.").length == 2) {
// 			    System.err.println("Suspicious non-10 dir found: " + filename);
// 			    System.err.println("  fileSize=" + fileSize);
// 			    System.err.println("  crc=" + crc);
// 			    System.err.println("  dots in name: " + (filename.split("\\.").length-1));
// 			}
			if(fileType == ENTRY_TYPE_FILE)
			    fileEntries.add(data);

			if(fileType != ENTRY_TYPE_DIR && fileType != ENTRY_TYPE_FILE) {
			    System.err.println("NEW fileType FOUND: 0x" + Util.toHexStringBE(fileType));
			    stdin.readLine();
			}
			//System.err.println("  " + filename.split("\\.").length);
			if(difference > 0)
			    System.out.println(" (array extended)");
			else
			    System.out.println();

			if(fileType == ENTRY_TYPE_FILE && outDir != null && fileSize != 0 && crc != 0) {
			    File outFile = new File(outDir, filename);
			    File outParent = outFile.getParentFile();
			    if(outParent != null && ((outParent.exists() && outParent.isDirectory()) ||
						     outParent.mkdirs())) {
				int seekIndexInBuffer = i+signature.length+post.length+stringSize+0x5;
				long seekPosition = (raf.getFilePointer()-lastReadPos)+seekIndexInBuffer;
				if(seekPosition+fileSize > raf.length()) {
				    System.out.println("ERROR: Invalid file size for \"" + filename + "\" (" + fileSize + " bytes)");
				    badFilesizeFiles.add(filename);
				}
				else {
				    RandomAccessFile outRaf = new RandomAccessFile(outFile, "rw");
				    System.out.println("input.getFilePointer() = " + raf.getFilePointer());
				    System.out.println("readAhead = " + (readAhead));
				    System.out.println("bytesRead = "+ bytesRead);
				    System.out.println("lastReadPos = "+ lastReadPos);
				    System.out.println("i = " + i);
				    byte[] mycrc32 = copyData(raf, seekPosition, outRaf, 0, fileSize);
				    outRaf.close();
				    System.out.println("Their digest: 0x" + Util.toHexStringBE(crc));
				    System.out.println("My digest   : 0x" + Util.byteArrayToHexString(mycrc32));
				    //stdin.readLine();
				    if(Util.arraysEqual(Util.toByteArrayBE(crc), mycrc32)) {
					System.out.println("Digests are equal.");
					extractedFiles.addLast(filename);
					/* Här bör vi flytta fram filpekaren till precis efter den extraherade filen,
					   för att undvika att titta i rar filer som är lagrade inuti andra rar-filer,
					   samt för att snabba upp extraheringen. */
					long oldFilePointer = raf.getFilePointer();
					long newFilePointer = seekPosition+fileSize-readAhead;
					if(newFilePointer > oldFilePointer) {
					    raf.seek(newFilePointer);
					    bytesRead = raf.read(buffer);
					    lastReadPos = bytesRead;
					    totalBytesRead += (raf.getFilePointer() - oldFilePointer);
					    i = pre.length;
					}
					else {
					    System.out.print("Situation where newFilePointer <= oldFilePointer har occurred!");
					    //stdin.readLine();
					}
				    }
				    else {
					System.out.println("NOT EQUAL! :(:(:(:(");
					if(deleteOnError) {
					    while(!outFile.delete()) {
						System.out.print("Could not delete file \"" + outFile + "\" try again (y/n)? ");
						String answer = stdin.readLine();
						while(!(answer.trim().equalsIgnoreCase("y") || answer.trim().equalsIgnoreCase("n"))) {
						    System.out.print("Ivalid answer. Answer again: (y/n)? ");
						    answer = stdin.readLine();
						}
					    
						if(answer.trim().equalsIgnoreCase("n"))
						    break;
					    }
					}
					badFiles.addLast(filename);
					//stdin.readLine();
				    }
				}
				//stdin.readLine();
			    }
			    else {
				System.out.println("Failed to create \"" + outFile + "\"");
				createErrorFiles.addLast(filename);
			    }
			}
		    } catch(Exception e) {
			e.printStackTrace();
			String answer;
			do {
			    System.out.print("Do you want to continue (y/n)? ");
			    answer = stdin.readLine();
			} while(!(answer.trim().equalsIgnoreCase("y") || answer.trim().equalsIgnoreCase("n")));
			
			if(answer.trim().equalsIgnoreCase("n"))
			    System.exit(0);
		    }
		}
	    }
	    /* Vi vill nu flytta (readAhead) bytes data från slutet av den fyllda delen av arrayen till början. */
	    System.arraycopy(buffer, lastReadPos-readAhead, buffer, 0, readAhead);
	    
	    /* Läs in nya data i arrayen, men skriv inte över dess inledande (readAhead) bytes */
	    bytesRead = raf.read(buffer, readAhead, buffer.length-readAhead);
	    totalBytesRead += bytesRead;
	    lastReadPos = readAhead+bytesRead;
	}
	System.out.println();
	System.err.println("Found " + signatureCount + " signatures.");
	System.err.println("Found " + savedEntries.size() + " entries.");
	if(savedEntries.size() > 0) {
	    System.err.println("Running analysis on all entries...");
	    Region[] similarRegions = findSimilarities(savedEntries);
	    byte[] entry = savedEntries.get(0);
	    System.out.println("First entry: 0x" + Util.byteArrayToHexString(entry));
	    LinkedList<byte[]> regionData = new LinkedList<byte[]>();
	    for(Region r : similarRegions) {
		byte[] currentRegion = new byte[(int)((r.endOffset+1)-r.startOffset)];
		System.arraycopy(entry, (int)r.startOffset, currentRegion, 0, currentRegion.length);
		System.out.println(r + ": 0x" + Util.byteArrayToHexString(currentRegion));
		regionData.addLast(currentRegion);
	    }
	}
	if(dirEntries.size() > 0) {
	    System.err.println("Running analysis on directories only...");
	    Region[] similarRegions = findSimilarities(dirEntries);
	    byte[] entry = dirEntries.get(0);
	    LinkedList<byte[]> regionData = new LinkedList<byte[]>();
	    for(Region r : similarRegions) {
		byte[] currentRegion = new byte[(int)((r.endOffset+1)-r.startOffset)];
		System.arraycopy(entry, (int)r.startOffset, currentRegion, 0, currentRegion.length);
		System.out.println(r + ": 0x" + Util.byteArrayToHexString(currentRegion));
		regionData.addLast(currentRegion);
	    }
	}
	if(fileEntries.size() > 0) {
	    System.err.println("Running analysis on files only...");
	    Region[] similarRegions = findSimilarities(fileEntries);
	    byte[] entry = fileEntries.get(0);
	    LinkedList<byte[]> regionData = new LinkedList<byte[]>();
	    for(Region r : similarRegions) {
		byte[] currentRegion = new byte[(int)((r.endOffset+1)-r.startOffset)];
		System.arraycopy(entry, (int)r.startOffset, currentRegion, 0, currentRegion.length);
		System.out.println(r + ": 0x" + Util.byteArrayToHexString(currentRegion));
		regionData.addLast(currentRegion);
	    }
	}

	if(badFiles.size() > 0) {
	    System.out.println("The following " + badFiles.size() + " files failed the CRC-32 checksum test" + (deleteOnError?" and were not extracted":"") + ":");
	    for(String filename : badFiles)
		System.out.println(" \"" + filename + "\"");
	}
	if(badFilesizeFiles.size() > 0) {
	    System.out.println("The following " + badFilesizeFiles.size() + " files could not be extracted due to incorrectly specified file size:");
	    for(String filename : badFilesizeFiles)
		System.out.println(" \"" + filename + "\"");
	}
	if(createErrorFiles.size() > 0) {
	    System.out.println("The following " + createErrorFiles.size() + " files could not be created:");
	    for(String filename : createErrorFiles)
		System.out.println(" \"" + filename + "\"");
	}
	
	System.out.println("Read " + totalBytesRead + " bytes from input file.");
    }

    /** Returns the calculated CRC-32 checksum of the copied data. */
    public static byte[] copyData(RandomAccessFile input, long inputPos, RandomAccessFile output, long outputPos, long fileSize) throws IOException {
	CRC32 crc32Digest = new CRC32();//MessageDigest.getInstance("CRC32");
	long oldFilePointer = input.getFilePointer();
	byte[] otherBuffer = new byte[4096];
	input.seek(inputPos);
	System.out.println("Seeking to " + input.getFilePointer() + ".");
	int totalBytesRead2 = 0;
	System.out.println("otherBuffer.length="+otherBuffer.length+" fileSize="+fileSize+" totalBytesRead2="+totalBytesRead2);
	System.out.println("input.read("+otherBuffer+", "+0+", " + (totalBytesRead2+otherBuffer.length<fileSize?otherBuffer.length:fileSize-totalBytesRead2) + ");");
	int bytesRead2 = input.read(otherBuffer, 0, (totalBytesRead2+otherBuffer.length<fileSize?otherBuffer.length:(int)(fileSize-totalBytesRead2)));
	totalBytesRead2 += bytesRead2;
	while(totalBytesRead2 < fileSize) {
	    crc32Digest.update(otherBuffer, 0, bytesRead2);
	    output.write(otherBuffer, 0, bytesRead2);
	    bytesRead2 = input.read(otherBuffer, 0, (totalBytesRead2+otherBuffer.length<fileSize?otherBuffer.length:(int)(fileSize-totalBytesRead2)));
	    totalBytesRead2 += bytesRead2;
	}
				
	crc32Digest.update(otherBuffer, 0, bytesRead2);
	output.write(otherBuffer, 0, bytesRead2);
	System.out.println("Extracted contents!");// Check them and press enter...");
	input.seek(oldFilePointer);
	return Util.toByteArrayBE((int)(crc32Digest.getValue() & 0xFFFFFFFF));
    }
    public static Region[] findSimilarities(List<byte[]> arrays) throws IOException {
	if(arrays.size() < 1)
	    return new Region[0];
	
	long minLength = Long.MAX_VALUE;
	boolean differentLengths = false;
	for(byte[] currentData : arrays) {
	    if(currentData.length < minLength) {
		if(minLength != Long.MAX_VALUE)
		    differentLengths = true;
		minLength = currentData.length;
	    }
	}
	if(differentLengths)
	    System.err.println("WARNING: Input arrays are not of equal length...");
	    
	LinkedList<Region> regions = new LinkedList<Region>();
	    
	long regionStart = -1;
	for(int i = 0; i < minLength; ++i) {
	    int lastByte = -1;
	    int[] values = new int[arrays.size()];
	    for(int j = 0; j < arrays.size(); ++j)
		values[j] = arrays.get(j)[i];//files[j].read();
		
	    for(int currentByte : values) {
		//System.out.print("0x" + Integer.toHexString(currentByte) + " == ");
		if(lastByte == -1)
		    lastByte = currentByte;
		else if(currentByte != lastByte) {
		    lastByte = -1;
		    //System.out.println("FALSE");
		    break;
		}
	    }
// 	    if(lastByte != -1)
// 		System.out.println("TRUE");
	    if(lastByte == -1 && regionStart != -1) {
		//System.out.println("Region concluded at 0x" + Long.toHexString(i));
		regions.add(new Region(regionStart, i-1));
		regionStart = -1;
	    }
	    else if(lastByte != -1 && regionStart == -1) {
		//System.out.println("New region started at 0x" + Long.toHexString(i));
		regionStart = i;
	    }
	}
	    
	System.out.println("Found " + regions.size() + " similar regions.");
	long totalSimilarBytes = 0;
	for(Region r : regions) {
	    long regionLength = r.endOffset-r.startOffset+1;
	    System.out.println("  0x" + Long.toHexString(r.startOffset) + "-0x" + Long.toHexString(r.endOffset) + " (" + regionLength + " B)");
	    totalSimilarBytes += regionLength;
	}
	System.out.println("Total matching bytes: " + totalSimilarBytes);
	return regions.toArray(new Region[regions.size()]);
    }

    private static class Region {
	public long startOffset;
	public long endOffset;

	public Region(long startOffset, long endOffset) {
	    this.startOffset = startOffset;
	    this.endOffset = endOffset;
	}
    }
}

class TestModel {
    public static void main(String[] args) {
	byte[] buffer = new byte[4096];
	int readAhead = 32;
	fill(buffer);
	while(true) {
	    for(int i = 0; i < buffer.length-readAhead; ++i) {
		
	    }

	    // Vi har gått igenom buffer från 0 till n-readAhead
	    // Kopiera då readAhead bytes från slutet till början och fyll
	    System.arraycopy(buffer, buffer.length-readAhead, buffer, 0, readAhead);
	    fill(buffer, readAhead, buffer.length-readAhead);
	}	
    }

    public static int alternate(byte[] buffer) {
	fill(buffer);
	while(true) {
	    for(int i = 0; i < buffer.length; ++i)
		dosomething(buffer[i]);
	    fill(buffer);
	}
    }

    public static void fill(byte[] buffer) {}
    public static void fill(byte[] buffer, int i, int j) {}
    public static void dosomething(byte b) {}
}
