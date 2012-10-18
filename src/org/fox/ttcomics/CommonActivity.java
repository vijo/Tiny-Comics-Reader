package org.fox.ttcomics;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
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
	
	private boolean m_storageAvailable;
	private boolean m_storageWritable;

	private SQLiteDatabase m_readableDb;
	private SQLiteDatabase m_writableDb;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	m_prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	
    	initDatabase();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		String state = Environment.getExternalStorageState();
		
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    m_storageAvailable = true;
		    m_storageWritable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			m_storageAvailable = true;
			m_storageWritable = false;
		} else {
			m_storageAvailable = false;
			m_storageWritable = false;
		}
		
    	if (m_prefs.getBoolean("use_position_sync", false)) {
        	String googleAccount = getGoogleAccount();
        	
	    	if (googleAccount != null) {
	    		m_syncClient.setOwner(googleAccount);    			
	    	} else {
	    		if (Build.FINGERPRINT.startsWith("generic")) {		    		
		    		m_syncClient.setOwner("TEST-ACCOUNT");	    			
	    		} else {
	    			m_syncClient.setOwner(null);
	    			toast(R.string.error_sync_no_account);
	    			    		
	    			SharedPreferences.Editor editor = m_prefs.edit();
	    			editor.putBoolean("use_position_sync", false);
	    			editor.commit();
	    		}
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

	public Cursor findComicByFileName(String fileName) {
		File file = new File(fileName);

		Cursor c = getReadableDb().query("comics_cache", null,
				"filename = ? AND path = ?",
				new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);

		if (c.moveToFirst()) {
			return c;
		} else {
			c.close();
			
			SQLiteStatement stmt = getWritableDb().compileStatement("INSERT INTO comics_cache " +
					"(filename, path, position, max_position, size, checksum) VALUES (?, ?, 0, 0, -1, '')");
			stmt.bindString(1, file.getName());
			stmt.bindString(2, file.getParentFile().getAbsolutePath());
			stmt.execute();
			
			c = getReadableDb().query("comics_cache", null,
					"filename = ? AND path = ?",
					new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);
			
			if (c.moveToFirst()) {
				return c;
			} else {
				c.close();
			}
		}

		return null;
	}
	
	public void setSize(String fileName, int size) {
		Cursor c = findComicByFileName(fileName);
		
		if (c != null) {
			c.close();
			
			File file = new File(fileName);
			
			SQLiteStatement stmt = getWritableDb().compileStatement("UPDATE comics_cache SET size = ? WHERE filename = ? AND path = ?");
			stmt.bindLong(1, size);
			stmt.bindString(2, file.getName());
			stmt.bindString(3, file.getParentFile().getAbsolutePath());
			stmt.execute();
			stmt.close();
		}
	}

	public void setChecksum(String fileName, String checksum) {
		Cursor c = findComicByFileName(fileName);
		
		if (c != null) {
			c.close();
			
			File file = new File(fileName);
			
			SQLiteStatement stmt = getWritableDb().compileStatement("UPDATE comics_cache SET checksum = ? WHERE filename = ? AND path = ?");
			stmt.bindString(1, checksum);
			stmt.bindString(2, file.getName());
			stmt.bindString(3, file.getParentFile().getAbsolutePath());
			stmt.execute();
			stmt.close();
		}
	}

	public void setLastPosition(String fileName, int position) {
		int lastPosition = getLastPosition(fileName);
		
		Cursor c = findComicByFileName(fileName);
		
		if (c != null) {
			c.close();

			File file = new File(fileName);
			
			SQLiteStatement stmt = getWritableDb().compileStatement("UPDATE comics_cache SET position = ?, max_position = ? WHERE filename = ? AND path = ?");
			stmt.bindLong(1, position);
			stmt.bindLong(2, Math.max(position, lastPosition));
			stmt.bindString(3, file.getName());
			stmt.bindString(4, file.getParentFile().getAbsolutePath());
			stmt.execute();
			stmt.close();
		}
		
	}
	
	public void setLastMaxPosition(String fileName, int position) {

		Cursor c = findComicByFileName(fileName);
		
		if (c != null) {
			c.close();

			File file = new File(fileName);
			
			SQLiteStatement stmt = getWritableDb().compileStatement("UPDATE comics_cache SET max_position = ? WHERE filename = ? AND path = ?");
			stmt.bindLong(1, position);
			stmt.bindString(2, file.getName());
			stmt.bindString(3, file.getParentFile().getAbsolutePath());
			stmt.execute();
			stmt.close();
		}	
	}

	public String getChecksum(String fileName) { 
		String checksum = null;
		
		File file = new File(fileName);
		
		Cursor c = getReadableDb().query("comics_cache", new String[] { "checksum" },
				"filename = ? AND path = ?",
				new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);

		if (c.moveToFirst()) {
			checksum = c.getString(c.getColumnIndex("checksum"));
		}

		c.close();
		
		return checksum;

	}

	
	public int getLastPosition(String fileName) { 
		int position = 0;
		
		File file = new File(fileName);
		
		Cursor c = getReadableDb().query("comics_cache", new String[] { "position" },
				"filename = ? AND path = ?",
				new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);

		if (c.moveToFirst()) {
			position = c.getInt(c.getColumnIndex("position"));
		}

		c.close();
		
		return position;

	}

	public int getCachedItemCount(String baseDir) {
		Cursor c = getReadableDb().query("comics_cache", new String[] { "COUNT(*)" },
				"path = ?",
				new String[] { baseDir }, null, null, null);
		
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		
		return count;
	}
	
	public int getMaxPosition(String fileName) { 
		int position = 0;
		
		File file = new File(fileName);
		
		Cursor c = getReadableDb().query("comics_cache", new String[] { "max_position" },
				"filename = ? AND path = ?",
				new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);

		if (c.moveToFirst()) {
			position = c.getInt(c.getColumnIndex("max_position"));
		}

		c.close();
		
		return position;
	}

	public int getSize(String fileName) { 
		int size = -1;
		
		File file = new File(fileName);
		
		Cursor c = getReadableDb().query("comics_cache", new String[] { "size" },
				"filename = ? AND path = ?",
				new String[] { file.getName(), file.getParentFile().getAbsolutePath() }, null, null, null);

		if (c.moveToFirst()) {
			size = c.getInt(c.getColumnIndex("size"));
		}

		c.close();
		
		//Log.d(TAG, "getSize:" + fileName + "=" + size);
		
		return size;
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
	
	public boolean isStorageAvailable() {
		return m_storageAvailable;
	}
	
	public boolean isStorageWritable() {
		return m_storageWritable;
	}
	
	private void initDatabase() {
		DatabaseHelper dh = new DatabaseHelper(getApplicationContext());
		
		m_writableDb = dh.getWritableDatabase();
		m_readableDb = dh.getReadableDatabase();
	}
	
	public synchronized SQLiteDatabase getReadableDb() {
		return m_readableDb;
	}

	public synchronized SQLiteDatabase getWritableDb() {
		return m_writableDb;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		m_readableDb.close();
		m_writableDb.close();
	}

	public void selectPreviousComic() {
		ComicPager frag = (ComicPager) getSupportFragmentManager().findFragmentByTag(FRAG_COMICS_PAGER);
		
		if (frag != null && frag.isAdded() && frag.getPosition() > 0 && frag.isPagingEnabled()) {
			frag.setCurrentItem(frag.getPosition() - 1);
		}		
	}

	public void selectNextComic() {
		ComicPager frag = (ComicPager) getSupportFragmentManager().findFragmentByTag(FRAG_COMICS_PAGER);
		
		if (frag != null && frag.isAdded() && frag.getPosition() < frag.getCount()-1 && frag.isPagingEnabled()) {
			frag.setCurrentItem(frag.getPosition() + 1);
		}		
		
		
	}

	public void cleanupSqliteCache(String baseDir) {
		Cursor c = getReadableDb().query("comics_cache", null,
				null, null, null, null, null);
		
		if (c.moveToFirst()) {
			while (!c.isAfterLast()) {
				int id = c.getInt(c.getColumnIndex(BaseColumns._ID));
				String fileName = c.getString(c.getColumnIndex("path")) + "/" + c.getString(c.getColumnIndex("filename"));

				File file = new File(fileName);

				if (!file.exists()) {
					SQLiteStatement stmt = getWritableDb().compileStatement("DELETE FROM comics_cache WHERE " + BaseColumns._ID + " = ?");
					stmt.bindLong(1, id);
					stmt.execute();
				}

				c.moveToNext();
			}
		}
		
		c.close();
		
		
	}

}
