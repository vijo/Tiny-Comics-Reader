package org.fox.ttcomics;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		addPreferencesFromResource(R.xml.preferences);
		
		if (CommonActivity.isCompatMode()) {
			Preference pref = findPreference("dim_status_bar");
			PreferenceCategory cat = (PreferenceCategory) findPreference("prefs_reading");
			cat.removePreference(pref);			
		}
	}
}
