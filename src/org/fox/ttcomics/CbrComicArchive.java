package org.fox.ttcomics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipFile;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import android.util.Log;

public class CbrComicArchive extends ComicArchive {
	private final String TAG = this.getClass().getSimpleName();
	
	private Archive m_archive;	
	private ArrayList<FileHeader> m_entries = new ArrayList<FileHeader>();
	
	@Override
	public int getCount() {
		return m_entries.size();
	}
	
	@Override
	public InputStream getItem(int index) throws IOException {
		try {
			return m_archive.getInputStream(m_entries.get(index));
		} catch (RarException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public CbrComicArchive(String fileName) throws IOException, RarException {
		Log.d(TAG, "CbrComicArchive: " + fileName);
		
		m_archive = new Archive(new File(fileName));

		FileHeader header = m_archive.nextFileHeader();
		
		while (header != null) {
			if (!header.isDirectory()) {
				String name = header.isUnicode() ? header.getFileNameW() : header.getFileNameString();
				
				if (name.toLowerCase().matches(".*\\.(jpg|bmp|gif)$")) {
					m_entries.add(header);
				}				
			}
			
			header = m_archive.nextFileHeader();
		}

		Collections.sort(m_entries, new Comparator<FileHeader>() {
		    public int compare(FileHeader a, FileHeader b) {
		    	String nameA = a.isUnicode() ? a.getFileNameW() : a.getFileNameString();
		    	String nameB = b.isUnicode() ? b.getFileNameW() : b.getFileNameString();
		    	
		    	return nameA.compareTo(nameB);
		    }
		});
		
	}

}
