package org.fox.ttcomics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.catacombae.rarx.ListFilesInArchive;
import org.catacombae.rarx.NewFileHeader;
import org.catacombae.rarx.RARFileEntry;
import org.catacombae.rarx.RARFileEntryStream;

import android.util.Log;

public class CbrComicArchive extends ComicArchive {
	private final String TAG = this.getClass().getSimpleName();
	
	private ZipFile m_zipFile;
	private ArrayList<RARFileEntry> m_entries = new ArrayList<RARFileEntry>();
	
	@Override
	public int getCount() {
		return m_entries.size();
	}
	
	@Override
	public InputStream getItem(int index) throws IOException {
		//return m_zipFile.getInputStream(m_entries.get(index));
		RARFileEntryStream rfes = new RARFileEntryStream(m_entries.get(index));
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		
		
		return new ByteArrayInputStream(baos.toByteArray());
	}
	
	public CbrComicArchive(String fileName) throws IOException {
		Log.d(TAG, "CbrComicArchive: " + fileName);
		
		/* m_zipFile = new ZipFile(fileName);

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
		}); */
		
		File file = new File(fileName);
		
		for (RARFileEntry entry : ListFilesInArchive.listFiles(file)) {
			NewFileHeader header = entry.getHeader(0);
			
			if (header.getFilenameAsString().toLowerCase().matches(".*\\.(jpg|bmp|gif)$")) {				
				m_entries.add(entry);
			}
		}
		
	}

}
