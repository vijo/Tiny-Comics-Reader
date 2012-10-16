package org.fox.ttcomics;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class CommonActivity extends FragmentActivity {
	private final String TAG = this.getClass().getSimpleName();

	protected static final String FRAG_COMICS_PAGER = "comic_pager";
	protected static final String FRAG_COMICS_LIST = "comics_list";

	protected final static int REQUEST_SHARE = 1;
	protected static final int REQUEST_VIEWCOMIC = 2;

	protected SharedPreferences m_prefs;
	protected SyncClient m_syncClient = new SyncClient();

	private boolean m_smallScreenMode = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	m_prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
     	
    	if (m_prefs.getBoolean("use_position_sync", false)) {
        	String googleAccount = getGoogleAccount();
        	
	    	if (googleAccount != null) {
	    		m_syncClient.setOwner(googleAccount);    			
	    	} else {
	    		toast(R.string.error_sync_no_account);
	    		
	    		SharedPreferences.Editor editor = m_prefs.edit();
	    		editor.putBoolean("use_position_sync", false);
	    		editor.commit(); 
	    		
	    		//m_syncClient.setOwner("TEST-ACCOUNT");
	    	}
    	}
	}
	
	public static boolean isCompatMode() {
		return android.os.Build.VERSION.SDK_INT <= 10;		
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

	protected static String sha1(String s) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("SHA1");
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

	public String getCacheFileName(String fileName) {
		String hash = md5(fileName);
		
		File file = new File(getExternalCacheDir().getAbsolutePath() + "/" + hash + ".png");
		
		return file.getAbsolutePath();
	}
	
	public String getGoogleAccount() {
		AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		Account[] list = manager.getAccounts();

		for (Account account: list) {
		    if (account.type.equalsIgnoreCase("com.google")) {
		        return account.name;
		    }
		}
		return null;
	}
	
	public void toast(int msgId) {
		Toast toast = Toast.makeText(CommonActivity.this, msgId, Toast.LENGTH_SHORT);
		toast.show();
	}

	public void toast(String msg) {
		Toast toast = Toast.makeText(CommonActivity.this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        if (width > height) {
	            inSampleSize = Math.round((float)height / (float)reqHeight);
	        } else {
	            inSampleSize = Math.round((float)width / (float)reqWidth);
	        }
	    }
	    return inSampleSize;
	}
	
	
	public void cleanupCache(boolean deleteAll) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File cachePath = getExternalCacheDir();
		
			long now = new Date().getTime();
			
			if (cachePath.isDirectory()) {
				for (File file : cachePath.listFiles()) {
					if (deleteAll || now - file.lastModified() > 1000*60*60*24*7) {
						file.delete();
					}					
				}				
			}
		}
	}
}
