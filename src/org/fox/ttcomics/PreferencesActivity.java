package org.fox.ttcomics;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;

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
		
		Preference dirPref = findPreference("comics_directory");
		dirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PreferencesActivity.this, DirectoryPicker.class);
				
				intent.putExtra(DirectoryPicker.START_DIR, prefs.getString("comics_directory", 
						Environment.getExternalStorageDirectory().getAbsolutePath()));
				
				startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);				
				return true;
             }
         });
		
		Preference clearPref = findPreference("clear_sync_data");
		clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
				builder.setMessage(R.string.dialog_clear_data_title)
				       .setCancelable(false)
				       .setPositiveButton(R.string.dialog_clear_data, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   
				        	 String googleAccount = getGoogleAccount();
				        	 SyncClient m_syncClient = new SyncClient();
				           	
				        	 if (googleAccount != null) {
				        		 m_syncClient.setOwner(googleAccount);    			
				        	 } else {
				        		 if (Build.HARDWARE.equals("goldfish")) {		    		
				        			 m_syncClient.setOwner("TEST-ACCOUNT");	    			
				        		 } else {				        		
				        			 m_syncClient.setOwner(null);

				        			 SharedPreferences.Editor editor = prefs.edit();
				        			 editor.putBoolean("use_position_sync", false);
				        			 editor.commit();
				 	    			
				        			 Toast toast = Toast.makeText(PreferencesActivity.this, R.string.error_sync_no_account, Toast.LENGTH_SHORT);
				        			 toast.show();
				        		 }
				        	 }
				        	 
				        	 if (m_syncClient.hasOwner()) {
				        		 m_syncClient.clearData();
				        	 }
				           }
				       })
				       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();			                
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
				
				return false;
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
}
