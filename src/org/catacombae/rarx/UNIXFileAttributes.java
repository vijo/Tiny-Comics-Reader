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

public class UNIXFileAttributes implements FileAttributes {
    private int attributes;
    
    public UNIXFileAttributes(int attributes) {
	this.attributes = attributes;
    }
    
    public boolean isDirectory() { return isDirectoryFlagSet(); }

    public boolean isFileFlagSet() { return Util.getBit(attributes, 15); }
    public boolean isDirectoryFlagSet() { return Util.getBit(attributes, 14); }
    public boolean isSpecialFileFlagSet() { return Util.getBit(attributes, 13); }
    public boolean isOwnerReadFlagSet() { return Util.getBit(attributes, 8); }
    public boolean isOwnerWriteFlagSet() { return Util.getBit(attributes, 7); }
    public boolean isOwnerExecuteFlagSet() { return Util.getBit(attributes, 6); }
    public boolean isGroupReadFlagSet() { return Util.getBit(attributes, 5); }
    public boolean isGroupWriteFlagSet() { return Util.getBit(attributes, 4); }
    public boolean isGroupExecuteFlagSet() { return Util.getBit(attributes, 3); }
    public boolean isOthersReadFlagSet() { return Util.getBit(attributes, 2); }
    public boolean isOthersWriteFlagSet() { return Util.getBit(attributes, 1); }
    public boolean isOthersExecuteFlagSet() { return Util.getBit(attributes, 0); }
    
    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "UNIXFileAttributes:");
	printFields(ps, prefix);
    }
    public void printFields(PrintStream ps, String prefix) {
	ps.println(prefix + " isFileFlagSet: " + isFileFlagSet());
	ps.println(prefix + " isDirectoryFlagSet: " + isDirectoryFlagSet());
	ps.println(prefix + " isSpecialFileFlagSet: " + isSpecialFileFlagSet());
	ps.println(prefix + " isOwnerReadFlagSet: " + isOwnerReadFlagSet());
	ps.println(prefix + " isOwnerWriteFlagSet: " + isOwnerWriteFlagSet());
	ps.println(prefix + " isOwnerExecuteFlagSet: " + isOwnerExecuteFlagSet());
	ps.println(prefix + " isGroupReadFlagSet: " + isGroupReadFlagSet());
	ps.println(prefix + " isGroupWriteFlagSet: " + isGroupWriteFlagSet());
	ps.println(prefix + " isGroupExecuteFlagSet: " + isGroupExecuteFlagSet());
	ps.println(prefix + " isOthersReadFlagSet: " + isOthersReadFlagSet());
	ps.println(prefix + " isOthersWriteFlagSet: " + isOthersWriteFlagSet());
	ps.println(prefix + " isOthersExecuteFlagSet: " + isOthersExecuteFlagSet());
// 	ps.println(prefix + " isFlagSet: " + isFlagSet());
    }
}
