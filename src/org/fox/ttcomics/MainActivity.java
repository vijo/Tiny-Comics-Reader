package org.fox.ttcomics;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

public class MainActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();
	
	private TabListener m_tabListener = new TabListener();
	
	private class TabListener implements ActionBar.TabListener {

		public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub			
		}

		public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
			Log.d(TAG, "POS=" + tab.getPosition());
			
			FragmentTransaction sft = getSupportFragmentManager().beginTransaction();
			
			switch (tab.getPosition()) {
			case 0:
				sft.replace(R.id.comics_list, new ComicListFragment(), FRAG_COMICS_LIST);				
				break;
			case 1:
				sft.replace(R.id.comics_list, new ComicListFragment(1), FRAG_COMICS_LIST);
				break;
			case 2:
				sft.replace(R.id.comics_list, new ComicListFragment(2), FRAG_COMICS_LIST);
				break;			
			}
			
			sft.commit();
			
		}

		public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        setContentView(R.layout.activity_main);
        
        int tabIndex = 0;
        
    	if (savedInstanceState == null) {
    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    		
    		ft.replace(R.id.comics_list, new ComicListFragment(), FRAG_COMICS_LIST);
    		
    		ft.commit();    		
    	} else {
    		tabIndex = savedInstanceState.getInt("tabIndex");
    	}

    	ActionBar actionBar = getActionBar();
    	
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    	
    	actionBar.addTab(getActionBar().newTab()
    			.setText(R.string.tab_all_comics)
    			.setTabListener(m_tabListener));

    	actionBar.addTab(getActionBar().newTab()
    			.setText(R.string.tab_unread)
    			.setTabListener(m_tabListener));
    	
    	actionBar.addTab(getActionBar().newTab()
    			.setText(R.string.tab_finished)
    			.setTabListener(m_tabListener));

    	if (tabIndex != 0) {
    		actionBar.selectTab(actionBar.getTabAt(tabIndex));
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
    	
    	((ViewGroup)findViewById(R.id.comics_list)).setLayoutTransition(new LayoutTransition());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putInt("tabIndex", getActionBar().getSelectedTab().getPosition());
	}
	
}
