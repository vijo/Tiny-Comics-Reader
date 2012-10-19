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

public class Win32FileAttributes implements FileAttributes {
    private int attributes;
    
    public Win32FileAttributes(int attributes) {
	this.attributes = attributes;
    }
    
    // Inteface methods
    public boolean isDirectory() {
	return isDirectoryFlagSet();
    }
    
    public boolean isReadOnlyFlagSet() {
	return Util.getBit(attributes, 0);
    }
    public boolean isHiddenFlagSet() {
	return Util.getBit(attributes, 1);
    }
    public boolean isSystemFlagSet() {
	return Util.getBit(attributes, 2);
    }
    public boolean isLabelFlagSet() {
	return Util.getBit(attributes, 3);
    }
    public boolean isDirectoryFlagSet() {
	return Util.getBit(attributes, 4);
    }
    public boolean isArchiveFlagSet() {
	return Util.getBit(attributes, 5);
    }

    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "Win32FileAttributes:");
	printFields(ps, prefix);
    }
    public void printFields(PrintStream ps, String prefix) {
	ps.println(prefix + " isReadOnlyFlagSet: " + isReadOnlyFlagSet());
	ps.println(prefix + " isHiddenFlagSet: " + isHiddenFlagSet());
	ps.println(prefix + " isSystemFlagSet: " + isSystemFlagSet());
	ps.println(prefix + " isLabelFlagSet: " + isLabelFlagSet());
	ps.println(prefix + " isDirectoryFlagSet: " + isDirectoryFlagSet());
	ps.println(prefix + " isArchiveFlagSet: " + isArchiveFlagSet());
    }
}
