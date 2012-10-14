package org.fox.ttcomics;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;

public class CommonActivity extends FragmentActivity {
	private final String TAG = this.getClass().getSimpleName();

	protected static final String FRAG_COMICS_PAGER = "comic_pager";
	protected static final String FRAG_COMICS_LIST = "comics_list";
	
	public static final String THUMBNAIL_PATH = "/data/org.fox.ttcomics/thumbnails/";

	protected SharedPreferences m_prefs;

	private boolean m_smallScreenMode = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	m_prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
	}

	protected void setSmallScreen(boolean smallScreen) {
		Log.d(TAG, "m_smallScreenMode=" + smallScreen);
		m_smallScreenMode = smallScreen;
	}

	public boolean isSmallScreen() {
		return m_smallScreenMode;
	}

	
	public void onComicArchiveSelected(String fileName) {
		//
	}

	public void setSize(String fileName, int size) {
		SharedPreferences lastread = getSharedPreferences("lastread", 0);

		SharedPreferences.Editor editor = lastread.edit();    	
    	editor.putInt(fileName + ":size", size);
    	
    	editor.commit();

	}
	
	public void setLastPosition(String fileName, int position) {
    	SharedPreferences lastread = getSharedPreferences("lastread", 0);
    	
    	int lastPosition = getLastPosition(fileName);
    	
    	SharedPreferences.Editor editor = lastread.edit();    	
    	editor.putInt(fileName + ":last", position);
    	editor.putInt(fileName + ":max", Math.max(lastPosition, position));
    	
    	editor.commit();
	}
	
	public void setLastMaxPosition(String fileName, int position) {
    	SharedPreferences lastread = getSharedPreferences("lastread", 0);
    	
    	SharedPreferences.Editor editor = lastread.edit();    	
    	editor.putInt(fileName + ":max", position);
    	
    	editor.commit();
	}
	
	public int getLastPosition(String fileName) { 
		SharedPreferences lastread = getSharedPreferences("lastread", 0);
	
		return lastread.getInt(fileName + ":last", 0);
	}

	public int getMaxPosition(String fileName) { 
		SharedPreferences lastread = getSharedPreferences("lastread", 0);
	
		return lastread.getInt(fileName + ":max", 0);
	}

	public int getSize(String fileName) { 
		SharedPreferences lastread = getSharedPreferences("lastread", 0);
	
		return lastread.getInt(fileName + ":size", -1);
	}

    public void onComicSelected(String fileName, int position) {
    	setLastPosition(fileName, position);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_rescan:
			ComicListFragment frag = (ComicListFragment) getSupportFragmentManager().findFragmentByTag(FRAG_COMICS_LIST);
			
			if (frag != null && frag.isAdded()) {
				frag.rescan(true);
			}
			
			return true;
		case R.id.menu_settings:
			Intent intent = new Intent(CommonActivity.this,
					PreferencesActivity.class);
			startActivityForResult(intent, 0);
			return true;
		default:
			Log.d(TAG,
					"onOptionsItemSelected, unhandled id=" + item.getItemId());
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressWarnings("deprecation")
	public boolean isPortrait() {
		Display display = getWindowManager().getDefaultDisplay(); 
		
	    int width = display.getWidth();
	    int height = display.getHeight();
		
	    return width < height;
	}

	protected static String md5(String s) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        
	        return hexString.toString();
	        
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static String getCacheFileName(String fileName) {
		String hashedUrl = md5(fileName);
		
		File storage = Environment.getExternalStorageDirectory();
		
		File file = new File(storage.getAbsolutePath() + THUMBNAIL_PATH + "/" + hashedUrl + ".png");
		
		return file.getAbsolutePath();
	}
}
