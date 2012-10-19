package org.fox.ttcomics;

import java.io.File;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ShareActionProvider;

public class MainActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();
	
	private TabListener m_tabListener;
	private int m_selectedTab;
	private String m_baseDirectory = "";
	private String m_fileName = "";
	
	@SuppressLint("NewApi")
	private class TabListener implements ActionBar.TabListener {

		public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub			
		}

		public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
			FragmentTransaction sft = getSupportFragmentManager().beginTransaction();
			
			if (m_selectedTab != tab.getPosition() && m_selectedTab != -1) {
				
				ComicListFragment frag = new ComicListFragment(tab.getPosition()); 
				
				frag.setBaseDirectory(m_baseDirectory);
				
				sft.replace(R.id.comics_list, frag, FRAG_COMICS_LIST);
			}
			
			m_selectedTab = tab.getPosition();
			
			sft.commit();			
		}

		public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        setContentView(R.layout.activity_main);

        setProgressBarVisibility(false);
        
        setSmallScreen(findViewById(R.id.tablet_layout_hack) == null);
        
    	if (savedInstanceState == null) {
           	m_selectedTab = getIntent().getIntExtra("selectedTab", 0);
    		
           	Log.d(TAG, "selTab=" + m_selectedTab);

           	ComicListFragment frag = new ComicListFragment(m_selectedTab);

            if (getIntent().getStringExtra("baseDir") != null) {
            	m_baseDirectory = getIntent().getStringExtra("baseDir");
            	frag.setBaseDirectory(m_baseDirectory);
            }

    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    		ft.replace(R.id.comics_list, frag, FRAG_COMICS_LIST);
    		ft.commit();
    		
    		m_selectedTab = -1;
    	} else {
        	m_selectedTab = -1;
        	m_baseDirectory = savedInstanceState.getString("baseDir");
        	m_fileName = savedInstanceState.getString("fileName");
    	}
    	
    	if (!isCompatMode()) {
    		m_tabListener = new TabListener();
    		
	    	ActionBar actionBar = getActionBar();
	    	
	    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    	
	    	actionBar.addTab(getActionBar().newTab()
	    			.setText(R.string.tab_all_comics)
	    			.setTabListener(m_tabListener));
	
	    	actionBar.addTab(getActionBar().newTab()
	    			.setText(R.string.tab_unread)
	    			.setTabListener(m_tabListener));
	
	    	actionBar.addTab(getActionBar().newTab()
	    			.setText(R.string.tab_unfinished)
	    			.setTabListener(m_tabListener));
	
	    	actionBar.addTab(getActionBar().newTab()
	    			.setText(R.string.tab_read)
	    			.setTabListener(m_tabListener));
	
	    	if (savedInstanceState != null) {
	    		m_selectedTab = savedInstanceState.getInt("selectedTab");
	    	} else {
	    		m_selectedTab = getIntent().getIntExtra("selectedTab", 0);
	    	}
	    	
	   		actionBar.selectTab(actionBar.getTabAt(m_selectedTab));
	   		
	    	actionBar.setDisplayHomeAsUpEnabled(m_baseDirectory.length() > 0);
    	}
	
    	if (m_prefs.getString("comics_directory", null) == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_need_prefs_message)
			       .setCancelable(false)
			       .setPositiveButton(R.string.dialog_need_prefs_preferences, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			   			// launch preferences
			   			
			        	   Intent intent = new Intent(MainActivity.this,
			        			   PreferencesActivity.class);
			        	   startActivityForResult(intent, 0);
			           }
			       })
			       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();			                
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
    	
    	if (!isCompatMode()) {
    		((ViewGroup)findViewById(R.id.comics_list)).setLayoutTransition(new LayoutTransition());
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putInt("selectedTab", m_selectedTab);
		out.putString("baseDir", m_baseDirectory);
		out.putString("fileName", m_fileName);
	}
    
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (m_baseDirectory.length() > 0) {
				finish();
			}			
			return true;
		default:
			Log.d(TAG,
					"onOptionsItemSelected, unhandled id=" + item.getItemId());
			return super.onOptionsItemSelected(item);
		}
	}
    
    @Override
	public void onComicArchiveSelected(String fileName) {
		super.onComicArchiveSelected(fileName);
		
		File file = new File(fileName);
		
		if (file.isDirectory()) {
			Intent intent = new Intent(MainActivity.this,
					MainActivity.class);

			intent.putExtra("baseDir", fileName);
			intent.putExtra("selectedTab", m_selectedTab);

			startActivityForResult(intent, 0); 
			
			
			
		} else if (file.canRead()) {
			Intent intent = new Intent(MainActivity.this,
					ViewComicActivity.class);

			intent.putExtra("fileName", fileName);
			m_fileName = fileName;
			
			startActivityForResult(intent, REQUEST_VIEWCOMIC); 
		}
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == REQUEST_VIEWCOMIC) {
	    	Log.d(TAG, "finished viewing comic: " + m_fileName);
	    	
	    	if (m_prefs.getBoolean("use_position_sync", false) && m_syncClient.hasOwner()) {
	    		toast(R.string.sync_uploading);
	    		m_syncClient.setPosition(sha1(new File(m_fileName).getName()), getLastPosition(m_fileName));
	    	}
	    }
	    
	    System.gc();
	    
	    super.onActivityResult(requestCode, resultCode, intent);
	}
	
}
