package org.fox.ttcomics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		addPreferencesFromResource(R.xml.preferences);
		
		if (CommonActivity.isCompatMode()) {
			Preference dimPref = findPreference("dim_status_bar");
			PreferenceCategory readingCat = (PreferenceCategory) findPreference("prefs_reading");
			readingCat.removePreference(dimPref);			
		}
		
		Preference dirPref = (Preference) findPreference("comics_directory");
		dirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PreferencesActivity.this, DirectoryPicker.class);
				
				//intent.putExtra(DirectoryPicker.START_DIR, prefs.getString("comics_directory", 
				//		Environment.getExternalStorageDirectory().getAbsolutePath()));
				
				startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);				
				return true;
             }
         });
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
			
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("comics_directory", path);	    	
	    	editor.commit();
			
		}
	}
}
