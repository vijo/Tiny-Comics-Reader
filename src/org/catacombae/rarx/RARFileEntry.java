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

import java.util.ArrayList;
import java.io.File;

public class RARFileEntry {
    public static class RARFileEntryPart {
	public File f;
	public long offset;
	public NewFileHeader header;
	
	public RARFileEntryPart(File f, long offset, NewFileHeader header) {
	    this.f = f;
	    this.offset = offset;
	    this.header = header;
	}
    }
    
    private ArrayList<RARFileEntryPart> parts = new ArrayList<RARFileEntryPart>();
    
    public RARFileEntry(File f, long offset, NewFileHeader header) {
	RARFileEntryPart firstPart = new RARFileEntryPart(f, offset, header);
	parts.add(firstPart);
    }
    
    public void addPart(File f, long offset, NewFileHeader header) {
	parts.add(new RARFileEntryPart(f, offset, header));
    }
    
    public int getPartCount() { return parts.size(); }
    public long getPackedSize() {
	long size = 0;
	for(RARFileEntryPart current : parts)
	    size += current.header.getPackSize();
	return size;
    }

    public boolean hasIncomingData() {
	return parts.get(0).header.hasIncomingData();
    }
    public boolean hasOutgoingData() {
	return parts.get(parts.size()-1).header.hasOutgoingData();
    }

    public boolean isComplete() {
	return (!parts.get(0).header.hasIncomingData() && !parts.get(parts.size()-1).header.hasOutgoingData());
    }

    public int getFinalFileCRC() {
	return parts.get(parts.size()-1).header.getFileCRC();
    }
    
    public File getFile(int partNumber) { return parts.get(partNumber).f; }
    public long getOffset(int partNumber) { return parts.get(partNumber).offset; }
    public NewFileHeader getHeader(int partNumber) { return parts.get(partNumber).header; }
}
