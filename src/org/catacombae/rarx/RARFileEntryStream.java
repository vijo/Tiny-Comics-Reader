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

import org.catacombae.io.*;
import java.io.*;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class RARFileEntryStream implements RandomAccessStream {
    public static class InvalidRARFilenameException extends RuntimeException {
	private static final long serialVersionUID = 0x2F49329E49173487L;

	public InvalidRARFilenameException(String message) {
	    super(message);
	}
    }
    
    public static class RAREntrySegment {
	public File file;
	public RandomAccessFile raf;
	public long headerPos;
	public long headerLength;
	public long dataPos;
	public long dataLength;
	public RAREntrySegment nextSegment = null;

	public RAREntrySegment(File file, RandomAccessFile raf, long headerPos, long headerLength, long dataPos, long dataLength/*, RAREntrySegment nextSegment*/) {
	    this.file = file;
	    this.raf = raf;
	    this.headerPos = headerPos;
	    this.headerLength = headerLength;
	    this.dataPos = dataPos;
	    this.dataLength = dataLength;
	    //this.nextSegment = nextSegment;
	}
    }
    
    public class Interval {
	public RAREntrySegment segment;
	public long startOffset;
	public long endOffset;

	public Interval(RAREntrySegment segment, long startOffset, long endOffset) {
	    this.segment = segment;
	    this.startOffset = startOffset;
	    this.endOffset = endOffset;
	}
    }
    
    public class IntervalStructure {
	private LinkedList<Interval> intervals = new LinkedList<Interval>();
	
	public void addSegment(RAREntrySegment res, long startOffset, long endOffset) {
	    Interval newInterval = new Interval(res, startOffset, endOffset);
	    if(intervals.size() > 0) {
		Interval previous = intervals.getLast();
		previous.segment.nextSegment = newInterval.segment;
	    }
	    intervals.addLast(newInterval);
	}
	
	public RAREntrySegment getSegment(long offset) {
	    for(Interval i : intervals)
		if(offset >= i.startOffset && offset < i.endOffset)
		    return i.segment;
	    return null;
	}
	
	public Interval getInterval(long offset) {
	    for(Interval i : intervals)
		if(offset >= i.startOffset && offset < i.endOffset)
		    return i;
	    return null;
	}

	public RAREntrySegment[] getAllSegmentsUnordered() {
	    RAREntrySegment[] result = new RAREntrySegment[intervals.size()];
	    int j = 0;
	    for(Interval i : intervals)
		result[j++] = i.segment;
	    return result;
	}
    }

    public static boolean debug = false;
    private IntervalStructure intervalStructure;
    private RAREntrySegment currentSegment;
    private boolean closed = false;
    private long seekPointer;
    private long globalPointer = 0;
    private long segmentPointer = 0;
    private final long length;

    public RARFileEntryStream(RARFileEntry entry) throws FileNotFoundException, IOException {
	this(entry.getFile(0), entry.getOffset(0));
    }
    
    public RARFileEntryStream(File startFile, long pos) throws FileNotFoundException, IOException {
	//this.startFile = startFile;
	//this.pos = pos;
	
	LinkedList<RAREntrySegment> entrySegments = getSegments(startFile, pos, new LinkedList<RAREntrySegment>(), false);
// 	for(RAREntrySegment res : entrySegments)
// 	    res.raf.seek(0);
	if(entrySegments == null || entrySegments.size() < 1)
	    throw new FileNotFoundException();
	this.currentSegment = entrySegments.getFirst();
	
	long currentPos = 0;
	this.intervalStructure = new IntervalStructure();
	for(RAREntrySegment res : entrySegments) {
	    if(debug) System.out.println("Adding \"" + res.file + "\" at " + currentPos);
	    intervalStructure.addSegment(res, currentPos, currentPos+res.dataLength);
	    currentPos += res.dataLength;
	}
	length = currentPos;
	if(debug) System.out.println("Length of entry: " + length + " bytes");

	seekPointer = -2;
	updateSeekPosition();
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized void close() throws IOException {
	if(debug) System.out.println("RARFileEntryStream.close()");
	if(closed) throw new IOException("Stream closed.");
	
	closed = true;
	for(RAREntrySegment res : intervalStructure.getAllSegmentsUnordered())
	    res.raf.close();
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized long getFilePointer() throws IOException {
	return ((seekPointer == -1)?globalPointer:seekPointer);
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized long length() throws IOException {
	return length;
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized int read() throws IOException {
	if(closed) throw new IOException("Stream closed.");
	byte[] b = new byte[1];
	read(b, 0, 1);
	return b[0] & 0xFF;
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized int read(byte[] b) throws IOException {
	if(closed) throw new IOException("Stream closed.");
	return read(b, 0, b.length);
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
//  	boolean debug = true;
	if(debug) System.out.print("RARFileEntryStream.read(b, " + off + ", " + len + ") = ");
// 	System.out.println();
// 	System.out.println("(1)currentSegment.raf.getFilePointer()=" + currentSegment.raf.getFilePointer());
// 	System.out.println("(1)=seekPointer=" + seekPointer);
	if(closed) throw new IOException("Stream closed.");
	try {
	    updateSeekPosition();
	} catch(IOException ioe) {
	    if(debug) System.out.println("-1 (Couldn't update seek position)");
	    return -1;
	}

	long bytesRemainingInSegment = currentSegment.dataLength - segmentPointer;
	while(bytesRemainingInSegment == 0) {
	    if(nextSegment())
		bytesRemainingInSegment = currentSegment.dataLength - segmentPointer;
	    else {
		if(debug) System.out.println("-1 (Nothing to read)");
		return -1; // Nothing to read!
	    }
	}
	int bytesRead = 0;
	mainLoop:
	while(bytesRead < len) {
	    int remainingBytes = len-bytesRead;
	    int bytesToRead = (remainingBytes < bytesRemainingInSegment)?remainingBytes:(int)bytesRemainingInSegment;
	    
// 	    System.out.println("(2)currentSegment.raf.getFilePointer()=" + currentSegment.raf.getFilePointer());
	    int currentBytesRead = currentSegment.raf.read(b, off+bytesRead, bytesToRead);
	    if(currentBytesRead == -1)
		throw new IOException("Reached end of file unexpectedly for file \"" + currentSegment.file + "\"! (segmentPointer=" + segmentPointer + " globalPointer=" + globalPointer + " bytesRemainingInSegment=" + bytesRemainingInSegment + " bytesRead=" + bytesRead + " bytesToRead=" + bytesToRead + " currentSegment.raf.getFilePointer()=" + currentSegment.raf.getFilePointer() + " currentSegment.dataLength=" + currentSegment.dataLength + ")");
	    
	    bytesRead += currentBytesRead;
	    segmentPointer += currentBytesRead;
	    globalPointer += currentBytesRead;
	    //seekPointer += currentBytesRead;

	    bytesRemainingInSegment = currentSegment.dataLength - segmentPointer;
	    while(bytesRemainingInSegment == 0) {
		if(nextSegment())
		    bytesRemainingInSegment = currentSegment.dataLength - segmentPointer;
		else
		    break mainLoop; // No more bytes to read;
	    }
	}
	if(debug) System.out.println(bytesRead);
	return bytesRead;
    }
    
    /** @see java.io.RandomAccessFile */
    public synchronized void seek(long pos) throws IOException {
	if(debug) System.out.println("RARFileEntryStream.seek(" + pos + ")");
	if(closed) throw new IOException("Stream closed.");
	if(pos < 0) throw new IOException("Invalid position.");
	seekPointer = pos;
    }
    
    private synchronized void updateSeekPosition() throws IOException {
	if(debug) System.out.println("updateSeekPosition()");
// 	System.out.println("seekPointer=" + seekPointer);
// 	System.out.println("globalPointer=" + globalPointer);
// 	System.out.println("segmentPointer=" + segmentPointer);
	if(seekPointer == globalPointer)
	    seekPointer = -1;
	else if(seekPointer != -1) {
	    //if(seekPointer > length) seek(length-1);
	    if(seekPointer == -2)
		seekPointer = 0;
	
	    long posDiff = seekPointer-globalPointer;
	    long newSegmentPointer = segmentPointer + posDiff;
	    if(newSegmentPointer >= 0 &&
	       newSegmentPointer < currentSegment.dataLength) {
		if(debug) System.out.println("  Pointer is within segment.");
		globalPointer += posDiff;
		segmentPointer += posDiff;
		if(debug) System.out.println("  Seeking to " + (currentSegment.dataPos + segmentPointer) +".");
		currentSegment.raf.seek(currentSegment.dataPos + segmentPointer);
		if(debug) System.out.println("  Done.");
	    }
	    else {
		if(debug) System.out.println("  Pointer is OUTSIDE segment. We need to get a new one.");
		Interval newInterval = intervalStructure.getInterval(seekPointer);
		if(newInterval == null)
		    throw new IOException("No more data to read.");
		RAREntrySegment newSegment = newInterval.segment;
		newSegmentPointer = (seekPointer-newInterval.startOffset);
		if(debug) System.out.println("  Seeking to " + (currentSegment.dataPos + segmentPointer) +".");
		newSegment.raf.seek(newSegment.dataPos + newSegmentPointer);
		if(debug) System.out.println("  Done.");
	    
		// If we got here, all is well.
		globalPointer = seekPointer;
		segmentPointer = newSegmentPointer;
		currentSegment = newSegment;
	    }
	    seekPointer = -1;
	}
    }

    public synchronized boolean isClosed() {
	return closed;
    }
    
    public synchronized boolean reopen() {
	if(debug) System.out.println("RARFileEntryStream.reopen()");
	if(closed) {
	    try {
		for(RAREntrySegment res : intervalStructure.getAllSegmentsUnordered())
		    res.raf = new RandomAccessFile(res.file, "r");
		closed = false;
		seekPointer = -2;
		updateSeekPosition();
		//seek(0);
		return true;
	    } catch(Exception e) {}
	}
	return false;
    }

    /**
     * Changes forward to the next segment and sets the pointers.
     */
    private boolean nextSegment() throws IOException {
	if(debug) System.out.println("RARFileEntryStream.nextSegment()");
	RAREntrySegment nextSegment = currentSegment.nextSegment;
	if(nextSegment != null) {
	    if(debug) System.out.println("  nextSegment != null");
	    if(debug) System.out.println("  currentSegment: " + currentSegment);
	    if(debug) System.out.println("    file: " + currentSegment.file);
	    if(debug) System.out.println("    headerPos: " + currentSegment.headerPos);
	    if(debug) System.out.println("    headerLength: " + currentSegment.headerLength);
	    if(debug) System.out.println("    dataPos: " + currentSegment.dataPos);
	    if(debug) System.out.println("    dataLength: " + currentSegment.dataLength);
	    if(debug) System.out.println("  nextSegment: " + nextSegment);
	    if(debug) System.out.println("    file: " + nextSegment.file);
	    if(debug) System.out.println("    headerPos: " + nextSegment.headerPos);
	    if(debug) System.out.println("    headerLength: " + nextSegment.headerLength);
	    if(debug) System.out.println("    dataPos: " + nextSegment.dataPos);
	    if(debug) System.out.println("    dataLength: " + nextSegment.dataLength);
	    globalPointer += currentSegment.dataLength - segmentPointer;
	    segmentPointer = 0;
	    nextSegment.raf.seek(nextSegment.dataPos);
	    currentSegment = nextSegment;
	    return true;
	}
	else {
	    if(debug) System.out.println("  nextSegment == null");
	    return false;
	}
    }
    
    private static LinkedList<RAREntrySegment> getSegments(File file, long position, LinkedList<RAREntrySegment> list, boolean needsIncoming) throws IOException, FileNotFoundException {
	if(debug) System.out.println("Getting new entry segment: " + file);
	RandomAccessFile raf = new RandomAccessFile(file, "r");
	raf.seek(position);
	RARHeader rh = RARFile.readHeader(raf);
	if(rh instanceof NewFileHeader) {
	    NewFileHeader nfh = (NewFileHeader)rh;
	    if(//nfh.getMethod() == RARHeader.METHOD_UNCOMPRESSED && 
	       (needsIncoming?nfh.hasIncomingData():!nfh.hasIncomingData()) &&
	       !(nfh instanceof CommentHeader)) {
		RAREntrySegment res = new RAREntrySegment(file, raf, position, nfh.getSize(), position+nfh.getSize(), nfh.getPackSize());
		list.addLast(res);
		if(nfh.hasOutgoingData()) {
		    File nextFile = getNextFile(file);
		    if(nextFile == null)
			throw new FileNotFoundException();
		    long posInNextFile = getMatchingPosInFile(nextFile, nfh);
		    if(posInNextFile == -1)
			throw new FileNotFoundException();
		    getSegments(nextFile, posInNextFile, list, true);
		}
	    }
	}
	return list;
    }
    
    protected static File getNextFile(File rarFile) {
	String filename = rarFile.getName();
	String newFilename;
	if(Pattern.matches(".+\\.[P|p]art\\d+\\.rar", filename)) {
	    String numberString = "";
	    long partNumber = 0; // I'm not serious... (:
	    int charstep = 0;
	    while(true) {
		String currentNumber = filename.substring(filename.length()-5-charstep, filename.length()-4-charstep);
		try {
		    partNumber += Integer.parseInt(currentNumber)*Util.pow(10, charstep);
		} catch(NumberFormatException nfe) {
		    break;
		}
		++charstep;
	    }
	    int numberLength = charstep;
	    String newPartNumberString = "" + (partNumber + 1);
	    
	    // Add leading zeroes
	    while(newPartNumberString.length() < numberLength)
		newPartNumberString = "0" + newPartNumberString;
	    
	    newFilename = filename.substring(0, filename.length()-4-numberLength) + newPartNumberString + ".rar";
	}
	else if(Pattern.matches(".+\\.r((ar)|(\\d\\d)){1}", filename)) {
	    String lastTwo = filename.substring(filename.length()-2, filename.length());
	    String newLastTwo = "";
	    if(lastTwo.equals("ar"))
		newLastTwo = "00";
	    else {
		try {
		    int archiveNumber = Integer.parseInt(lastTwo);
		    newLastTwo = "" + (archiveNumber+1);
		    if(newLastTwo.length() < 2)
			newLastTwo = "0" + newLastTwo;
		} catch(NumberFormatException nfe) {
		    throw new RuntimeException(nfe);
		}
	    }
	    if(newLastTwo.length() != 2)
		throw new InvalidRARFilenameException("Incremented filename invalid.");
	    
	    newFilename = filename.substring(0, filename.length()-2) + newLastTwo;
	}
	else if(Pattern.matches(".+\\.\\d\\d\\d", filename)) {
	    String lastThree = filename.substring(filename.length()-3, filename.length());
	    String newLastThree = "";
	    try {
		int archiveNumber = Integer.parseInt(lastThree);
		newLastThree = "" + (archiveNumber+1);
		while(newLastThree.length() < 3)
		    newLastThree = "0" + newLastThree;
	    } catch(NumberFormatException nfe) {
		throw new RuntimeException(nfe);
	    }
	    if(newLastThree.length() != 3)
		throw new InvalidRARFilenameException("Incremented filename invalid.");
	    
	    newFilename = filename.substring(0, filename.length()-3) + newLastThree;
	}
	else
	    throw new InvalidRARFilenameException("Filename in argument invalid for a RAR-file.");
	
	File newFile = new File(rarFile.getParent(), newFilename);
	if(newFile.exists())
	    return newFile;
	else {
	    if(debug) System.out.println("Couldn't find \"" + newFile + "\"");
	    return null;
	}
    }

    private static long getMatchingPosInFile(File f, NewFileHeader currentHeader) {
	try {
	    RandomAccessFile raf = new RandomAccessFile(f, "r");
	    RARFile.readMarkHeader(raf);
	    RARHeader rh = RARFile.readHeader(raf);
	    while(rh != null) {
		if(rh instanceof NewFileHeader) {
		    NewFileHeader nextHeader = (NewFileHeader)rh;
		    if(nextHeader.getFilenameAsString().equals(currentHeader.getFilenameAsString()) &&
		       nextHeader.getUnpSize() == currentHeader.getUnpSize()) {
			long pos = raf.getFilePointer()-nextHeader.getPackSize()-nextHeader.getHeadSize();
			raf.close();
			return pos;
		    }
		}
		rh = RARFile.readHeader(raf);
	    }
	    raf.close();
	} catch(IOException ioe) {
	    ioe.printStackTrace();
	}
	return -1;
    }
    
    /** Testcode */
    public static void main(String[] args) throws IOException {
	if(args.length != 3)
	    System.out.println("RARFileEntryStream self test needs exactly 3 arguments...");
	String rarFilename = args[0];
	String entryName = args[1];
	String referenceFilename = args[2];
	
	System.out.println("Initiating self test of RARFileEntryStream...");
	System.out.println("  RAR file: " + rarFilename);
	System.out.println("  Entry name: " + entryName);
	System.out.println("  Reference file: " + referenceFilename);

	File rarFile = new File(rarFilename);
	File referenceFile = new File(referenceFilename);
	
	long position = RARFile.findEntry(entryName, new RandomAccessFile(rarFile, "r"));
	RARFileEntryStream stream = new RARFileEntryStream(rarFile, position);
	RandomAccessFile ref = new RandomAccessFile(referenceFile, "r");
	byte[] buffer1 = new byte[4096];
	byte[] buffer2 = new byte[4096];
	int bytesRead1;
	int bytesRead2;
	long totalBytesRead1;
	long totalBytesRead2;

	System.out.println("1. Deterministic tests...");
	if(stream.length() != ref.length())
	    System.out.println("Lengths not equal! stream: " + stream.length() + " reference file: " + ref.length());
	
	// Testing reading from the beginning
	ref.seek(0);
	stream.seek(0);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	bytesRead1 = ref.read(buffer1);
	bytesRead2 = stream.read(buffer2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	if(bytesRead1 != bytesRead2) throw new RuntimeException("Did not read equal amount of bytes. (bytesRead1="+bytesRead1+" bytesRead2="+bytesRead2+")");
	if(!Util.arraysEqual(buffer1, buffer2)) throw new RuntimeException("Arrays not equal!");

	// Going to the middle of the file
	ref.seek(ref.length()/2);
	stream.seek(stream.length()/2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	bytesRead1 = ref.read(buffer1);
	bytesRead2 = stream.read(buffer2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	if(bytesRead1 != bytesRead2) throw new RuntimeException("Did not read equal amount of bytes. (bytesRead1="+bytesRead1+" bytesRead2="+bytesRead2+")");
	if(!Util.arraysEqual(buffer1, buffer2)) throw new RuntimeException("Arrays not equal!");

	// Going to the end of the file
	ref.seek(ref.length());
	stream.seek(stream.length());
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	bytesRead1 = ref.read(buffer1);
	bytesRead2 = stream.read(buffer2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	if(bytesRead1 != bytesRead2) throw new RuntimeException("Did not read equal amount of bytes. (bytesRead1="+bytesRead1+" bytesRead2="+bytesRead2+")");
	if(!Util.arraysEqual(buffer1, buffer2)) throw new RuntimeException("Arrays not equal!");

	// Going beyond the end of the file
	ref.seek(ref.length()*2);
	stream.seek(stream.length()*2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	bytesRead1 = ref.read(buffer1);
	bytesRead2 = stream.read(buffer2);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	if(bytesRead1 != bytesRead2) throw new RuntimeException("Did not read equal amount of bytes. (bytesRead1="+bytesRead1+" bytesRead2="+bytesRead2+")");
	if(!Util.arraysEqual(buffer1, buffer2)) throw new RuntimeException("Arrays not equal!");
	
	// Comparing files byte for byte
	System.out.println("Comparing file data...");
	ref.seek(0);
	stream.seek(0);
	if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	totalBytesRead1 = 0;
	totalBytesRead2 = 0;
	while(totalBytesRead1 < ref.length() && totalBytesRead2 < stream.length()) {
	    System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
	    System.out.print("  " + totalBytesRead1 + " bytes processed");
	    long bytesRemaining1 = ref.length()-totalBytesRead1;
	    long bytesRemaining2 = ref.length()-totalBytesRead2;
	    bytesRead1 = ref.read(buffer1, 0, (bytesRemaining1 < buffer1.length?(int)bytesRemaining1:buffer1.length));
	    bytesRead2 = stream.read(buffer2, 0, (bytesRemaining2 < buffer2.length?(int)bytesRemaining2:buffer2.length));
	    if(bytesRead1 != bytesRead2) throw new RuntimeException("Have not read equal amount of bytes. (bytesRead1="+bytesRead1+" bytesRead2="+bytesRead2+")");
	    if(bytesRead1 == -1)
		break;
	    totalBytesRead1 += bytesRead1;
	    totalBytesRead2 += bytesRead2;
	    if(ref.getFilePointer() != stream.getFilePointer()) throw new RuntimeException("File pointers not equal! ref: " + ref.getFilePointer() + " stream: " + stream.getFilePointer());
	    if(totalBytesRead1 != totalBytesRead2) throw new RuntimeException("Have not read equal amount of bytes. (totalBytesRead1="+totalBytesRead1+" totalBytesRead2="+totalBytesRead2+")");
	    if(!Util.arraysEqual(buffer1, buffer2)) throw new RuntimeException("Arrays not equal!");
	}
	System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
	System.out.print("  " + totalBytesRead1 + " bytes processed");
    }
}
