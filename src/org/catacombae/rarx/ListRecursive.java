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

import java.io.*;
import java.util.regex.Pattern;

public class ListRecursive {
    public static class RegexpFileFilter implements FileFilter {
	private String pattern;
	public RegexpFileFilter(String pattern) {
	    this.pattern = pattern;
	}
	public boolean accept(File f) {
	    return Pattern.matches(pattern, f.getName());
	}
    }
    
    public static void main(String[] args) throws IOException {
	File rootDir = null;
	String pattern = null;
	File outFile = null;
	System.out.println("args.length=" + args.length);
	if(args.length > 2) {
	    rootDir = new File(args[0]);
	    pattern = args[1];
	    outFile = new File(args[2]);
	    System.out.println("Writing to file " + outFile.getCanonicalPath());
	}
	else if(args.length > 1) {
	    rootDir = new File(args[0]);
	    pattern = args[1];
	}
	else if(args.length > 0)
	    pattern = args[0];
	else {
	    System.out.println("Could not parse arguments.");
	    return;
	}
	    
	if(rootDir == null)
	    rootDir = new File(System.getProperty("user.dir"));
	
	FileFilter fnf;
	if(pattern == null)
	    fnf = null;
	else
	    fnf = new RegexpFileFilter(pattern);

	if(outFile == null)
	    listRecursive(rootDir, fnf, System.out);
	else
	    listRecursive(rootDir, fnf, new PrintStream(new FileOutputStream(outFile), false, "UTF-8"));
    }

    public static void listRecursive(File rootDir, FileFilter fnf, PrintStream ps) {
	File[] allChildren = rootDir.listFiles();
	File[] matchingChildren;
	if(fnf == null)
	    matchingChildren = allChildren;
	else
	    matchingChildren = rootDir.listFiles(fnf);

	if(matchingChildren != null) {
	    for(File child : matchingChildren)
		ps.println(child);
	}
	
	if(allChildren != null) {
	    for(File child : allChildren)
		listRecursive(child, fnf, ps);
	}
    }
}
