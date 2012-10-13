package org.fox.ttcomics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	m_prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
	}
	
	public void onComicArchiveSelected(String fileName) {
		
		Intent intent = new Intent(CommonActivity.this,
		ViewComicActivity.class);

		intent.putExtra("fileName", fileName);

		startActivityForResult(intent, 0); 
		
		/* FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		
		ft.replace(R.id.comics_list, new ComicPager(fileName), FRAG_COMICS_LIST);
		ft.addToBackStack(null);
		
		ft.commit(); */    
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
				frag.rescan();
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
	
}
