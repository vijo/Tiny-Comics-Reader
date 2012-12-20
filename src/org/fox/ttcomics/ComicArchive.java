package org.fox.ttcomics;

import java.io.IOException;
import java.io.InputStream;

public abstract class ComicArchive {
	public abstract int getCount();
	public abstract InputStream getItem(int index) throws IOException;
	public boolean isValidComic(String fileName) {
		return fileName.toLowerCase().matches(".*\\.(jpg|bmp|gif|png)$");
	}
}
