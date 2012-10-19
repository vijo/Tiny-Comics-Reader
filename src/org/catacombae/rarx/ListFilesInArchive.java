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

public class ListFilesInArchive {
    public static void main(String[] args) {
	try {
	    File f = new File(args[0]);
	    RARFileEntry[] entries = listFiles(f);
	    for(RARFileEntry entry : entries) {
		NewFileHeader nfh = entry.getHeader(0);
		System.out.println(nfh.getFilenameAsString());
	    }
	} catch(InvalidDataException e) {
	    System.out.println("Invalid RAR-file. Message: " + e.getMessage());
	    System.out.println("File: \"" + args[0] + "\"");
	} catch(IOException ioe) {
	    ioe.printStackTrace();
	}
    }
    
    public static Tree<RARFileEntry> listFilesAsTree(File rarFile) throws IOException, InvalidDataException {
	return listFilesAsTree(rarFile, false);
    }
    public static Tree<RARFileEntry> listFilesAsTree(File rarFile, boolean includeComments) throws IOException, InvalidDataException {
	RARFileEntry[] contents = listFiles(rarFile, includeComments);
	
	// Create the empty directory tree
	Tree<RARFileEntry> rarTree = new Tree<RARFileEntry>();
	for(RARFileEntry currentEntry : contents) {
	    NewFileHeader nfh = currentEntry.getHeader(0);

	    String[] filenameComponents = nfh.getFilenameAsString().split("\\" + NewFileHeader.PATH_SEPARATOR);
	    Tree<RARFileEntry> currentNode = rarTree;
	    
	    // Loop through the first n-1 components and create tree entries when needed
	    for(int i = 0; i < filenameComponents.length-1; ++i) {
		TreeNode<RARFileEntry> childNode = currentNode.get(filenameComponents[i]);
		if(childNode == null) {
		    childNode = new Tree<RARFileEntry>();
		    currentNode.put(filenameComponents[i], childNode);
		}
		
		if(childNode instanceof Tree)
		    currentNode = (Tree<RARFileEntry>)childNode;
		else
		    throw new RuntimeException("Duplicate entries in RAR file");
	    }
	    
	    // The last component is mapped to the current RARFileEntry
	    TreeNode<RARFileEntry> childNode = currentNode.get(filenameComponents[filenameComponents.length-1]);
	    if(childNode == null) {
		if(nfh.getFileAttributesStructured().isDirectory())
		    childNode = new Tree<RARFileEntry>(currentEntry);
		else
		    childNode = new Leaf<RARFileEntry>(currentEntry);
		currentNode.put(filenameComponents[filenameComponents.length-1], childNode);
	    }
	    else if(nfh.getFileAttributesStructured().isDirectory()) {
		if(childNode.getValue() == null)
		    childNode.setValue(currentEntry);
		else
		    throw new RuntimeException("Duplicate entries in RAR file");
	    }
	    else
		throw new RuntimeException("Duplicate entries in RAR file");
	}
	
	// Should probably check that the directory structure is complete before passing it on
	
	return rarTree;
    }
    
    
    public static RARFileEntry[] listFiles(File rarFile) throws IOException, InvalidDataException {
	return listFiles(rarFile, false);
    }
    public static RARFileEntry[] listFiles(File rarFile, boolean includeComments) throws IOException, InvalidDataException {
	LinkedList<RARFileEntry> entries = new LinkedList<RARFileEntry>();
	RARFileEntry outgoingEntry = null;
	mainLoop:
	do {
	    //outgoingEntry = null;
	    RandomAccessFile raf = new RandomAccessFile(rarFile, "r");
	    
	    RARFile.readMarkHeader(raf);
	    //NewMainArchiveHeader nmah = RARFile.readNewMainArchiveHeader(raf);
	    //nmah.print(System.out, "");
	    long offset = raf.getFilePointer();
	    RARHeader rh = RARFile.readHeader(raf);
	    while(rh != null) {
		try {
		    if(rh instanceof NewFileHeader && (includeComments || !(rh instanceof CommentHeader))) {
			NewFileHeader nfh = (NewFileHeader)rh;
			if(!nfh.hasIncomingData()) {
			    RARFileEntry rfe = new RARFileEntry(rarFile, offset, nfh);
			    if(outgoingEntry != null && nfh.hasOutgoingData()) {
				throw new RuntimeException("Found two outgoing files in the same archive!");
			    }
			    else if(nfh.hasOutgoingData())
				outgoingEntry = rfe;
			    
			    entries.add(rfe);
			}
			else {
			    if(outgoingEntry != null) {
				outgoingEntry.addPart(rarFile, offset, nfh);
				if(!nfh.hasOutgoingData())
				    outgoingEntry = null;
			    }
			    else {
				// Reading incomplete entry.
				RARFileEntry rfe = new RARFileEntry(rarFile, offset, nfh);
				entries.add(rfe);
				if(nfh.hasOutgoingData())
				    outgoingEntry = rfe;
			    }
			}
			/*
			if(!nfh.hasIncomingData())
			    entries.add(nfh);
			if(nfh.hasOutgoingData()) {
			    if(outgoingEntry == null)
				outgoingEntry = nfh;
			    else
				throw new RuntimeException("Found two outgoing files in the same archive!");
			}
			*/
			//nfh.print(System.out, "");
		    }
		    offset = raf.getFilePointer();
		    rh = RARFile.readHeader(raf);
		} catch(IOException ioe) {
		    ioe.printStackTrace();
		    break mainLoop;
		}
	    }
	    if(outgoingEntry != null) {
		File oldRarFile = rarFile;
		rarFile = RARFileEntryStream.getNextFile(rarFile);
		if(rarFile == null)
		    throw new FileNotFoundException("Could not find successor to \"" + oldRarFile + "\"");
// 		long posInNextFile = RARFileEntryStream.getMatchingPosInFile(nextFile, nfh);
// 		if(posInNextFile == -1)
// 		    throw new FileNotFoundException();
// 		getSegments(nextFile, posInNextFile, list, true);
	    }
	} while(outgoingEntry != null);
	
	return entries.toArray(new RARFileEntry[entries.size()]);
    }
}
