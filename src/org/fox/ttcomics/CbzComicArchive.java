package org.fox.ttcomics;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CbzComicArchive extends ComicArchive {
	private final String TAG = this.getClass().getSimpleName();
	
	private ZipFile m_zipFile;
	private int m_count;
	private ArrayList<ZipEntry> m_entries = new ArrayList<ZipEntry>();
	
	@Override
	public int getCount() {
		return m_count;
	}
	
	@Override
	public InputStream getItem(int index) throws IOException {
		return m_zipFile.getInputStream(m_entries.get(index));
	}
	
	public CbzComicArchive(String fileName) throws IOException {
		m_zipFile = new ZipFile(fileName);

		Enumeration<? extends ZipEntry> e = m_zipFile.entries();

		while (e.hasMoreElements()) {
			ZipEntry ze = e.nextElement();		
			if (!ze.isDirectory() && ze.getName().toLowerCase().matches(".*\\.(jpg|bmp|gif)$")) {
				m_entries.add(ze);
				m_count++;
			}
		}
		
		Collections.sort(m_entries, new Comparator<ZipEntry>() {
		    public int compare(ZipEntry a, ZipEntry b) {
		        return a.getName().compareTo(b.getName());
		    }
		});
		
	}

}
