package org.fox.ttcomics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

public class ViewComicActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();

	private String m_fileName;
	private String m_tmpFileName;
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        setTheme(m_prefs.getBoolean("use_dark_theme", false) ? R.style.DarkTheme : R.style.AppTheme);
        
        setContentView(R.layout.activity_view_comic);
        
        if (savedInstanceState == null) {
        	m_fileName = getIntent().getStringExtra("fileName"); 
        	
       		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
       		ft.replace(R.id.comics_pager_container, new ComicPager(m_fileName), FRAG_COMICS_PAGER);
       		ft.commit();
        } else {
        	m_fileName = savedInstanceState.getString("fileName");
        	m_tmpFileName = savedInstanceState.getString("tmpFileName");
        }

        setOrientationLock(isOrientationLocked(), true);
        
       	getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        setTitle(new File(m_fileName).getName());

        if (m_prefs.getBoolean("use_full_screen", false)) {
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	
       		getSupportActionBar().hide();
        }
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_view_comic, menu);
        
        menu.findItem(R.id.menu_sync_location).setVisible(m_prefs.getBoolean("use_position_sync", false) && m_syncClient.hasOwner());
        
        return true;
    }
    
	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putString("fileName", m_fileName);
		out.putString("tmpFileName", m_tmpFileName);
	}
	
	@Override
	public void onComicSelected(String fileName, int position) {
		super.onComicSelected(fileName, position);
	}
	
	public void onPause() {
		super.onPause();
		
		// upload progress
		if (m_prefs.getBoolean("use_position_sync", false) && m_syncClient.hasOwner()) {
    		//toast(R.string.sync_uploading);
    		m_syncClient.setPosition(sha1(new File(m_fileName).getName()), getLastPosition(m_fileName));
    	}
	}
	
	private void shareComic() {
		
		ComicPager pager = (ComicPager) getSupportFragmentManager().findFragmentByTag(FRAG_COMICS_PAGER);
		
		if (pager != null) {
			
			try {
				File tmpFile = File.createTempFile("trcshare", ".jpg", getExternalCacheDir());
				
				Log.d(TAG, "FILE=" + tmpFile);
				
				InputStream is = pager.getArchive().getItem(pager.getPosition());
				
				FileOutputStream fos = new FileOutputStream(tmpFile);
				
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) != -1) {
				    fos.write(buffer, 0, len);
				}
				
				fos.close();
				is.close();

				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				
				shareIntent.setType("image/jpeg");
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));

				m_tmpFileName = tmpFile.getAbsolutePath();
				
				startActivityForResult(Intent.createChooser(shareIntent, getString(R.string.share_comic)), REQUEST_SHARE);
								
			} catch (IOException e) {
				toast(getString(R.string.error_could_not_prepare_file_for_sharing));
				e.printStackTrace();
			}

		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == REQUEST_SHARE) {
	    	File tmpFile = new File(m_tmpFileName);
	    	
	    	if (tmpFile.exists()) {
	    		tmpFile.delete();
	    	}

	    }
	    super.onActivityResult(requestCode, resultCode, intent);
	}
	
	protected boolean isOrientationLocked() {
		return m_prefs.getBoolean("prefs_lock_orientation", false);
	}
	
	private void setOrientationLock(boolean locked, boolean restoreLast) {
		if (locked) {
			
			int currentOrientation = restoreLast ? m_prefs.getInt("last_orientation", getResources().getConfiguration().orientation) :					
					getResources().getConfiguration().orientation;
			
			if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			} else {
			   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
			}
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}

		if (locked != isOrientationLocked()) {
			SharedPreferences.Editor editor = m_prefs.edit();
			editor.putBoolean("prefs_lock_orientation", locked);
			editor.putInt("last_orientation", getResources().getConfiguration().orientation);
			editor.commit();
		}
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share:
			shareComic();			
			return true;
		case R.id.menu_toggle_orientation_lock:
			setOrientationLock(!isOrientationLocked(), false);			
			return true;			
		case R.id.menu_sync_location:
	        m_syncClient.getPosition(sha1(new File(m_fileName).getName()), new SyncClient.PositionReceivedListener() {
				@Override
				public void onPositionReceived(final int position) {
					final ComicPager pager = (ComicPager) getSupportFragmentManager().findFragmentByTag(FRAG_COMICS_PAGER);
					
					if (pager != null && pager.isAdded()) {
						int localPosition = pager.getPosition();

						if (position > localPosition) {
							AlertDialog.Builder builder = new AlertDialog.Builder(ViewComicActivity.this);
							builder.setMessage(getString(R.string.sync_server_has_further_page, localPosition+1, position+1))
							       .setCancelable(false)
							       .setPositiveButton(R.string.dialog_open_page, new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							        	   pager.setCurrentItem(position);
							           }
							       })
							       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							                dialog.cancel();			                
							           }
							       });
							AlertDialog alert = builder.create();
							alert.show();

						} else {
							toast(R.string.error_sync_no_data);
						}
						
					}				
				}
			});
	        return true;			
		case R.id.menu_go_location:			
			Dialog dialog = new Dialog(ViewComicActivity.this);
			AlertDialog.Builder builder = new AlertDialog.Builder(ViewComicActivity.this)
					.setTitle("Go to...")
					.setItems(
							new String[] {
									getString(R.string.dialog_location_beginning),
									getString(R.string.dialog_location_furthest),
									getString(R.string.dialog_location_location),
									getString(R.string.dialog_location_end) 
									},
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									
									final ComicPager cp = (ComicPager) getSupportFragmentManager().findFragmentByTag(CommonActivity.FRAG_COMICS_PAGER);

									switch (which) {
									case 0:
										cp.setCurrentItem(0);										
										break;
									case 1:
										cp.setCurrentItem(getMaxPosition(m_fileName));
										break;
									case 2:										
										if (!isCompatMode()) {
											LayoutInflater inflater = getLayoutInflater();
											View contentView = inflater.inflate(R.layout.dialog_location, null);
	
											final NumberPicker picker = (NumberPicker) contentView.findViewById(R.id.number_picker); 
													
											picker.setMinValue(1);
											picker.setMaxValue(getSize(m_fileName));
											picker.setValue(cp.getPosition()+1);
	
											Dialog seekDialog = new Dialog(ViewComicActivity.this);
											AlertDialog.Builder seekBuilder = new AlertDialog.Builder(ViewComicActivity.this)
												.setTitle(R.string.dialog_open_location)
												.setView(contentView)
												.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
													
													public void onClick(DialogInterface dialog, int which) {
														dialog.cancel();													
													}
												}).setPositiveButton(R.string.dialog_open_location, new DialogInterface.OnClickListener() {
													
													public void onClick(DialogInterface dialog, int which) {
														dialog.cancel();
														
														cp.setCurrentItem(picker.getValue()-1);
														
													}
												});
											
											seekDialog = seekBuilder.create();
											seekDialog.show();
										} else {
											LayoutInflater inflater = getLayoutInflater();											
											final View contentView = inflater.inflate(R.layout.dialog_location_compat, null);
											
											final SeekBar seeker = (SeekBar) contentView.findViewById(R.id.number_seeker);
											final TextView pageNum = (TextView) contentView.findViewById(R.id.page_number);
											final int size = getSize(m_fileName); 
													
											seeker.setMax(size-1);
											seeker.setProgress(cp.getPosition());
	
											pageNum.setText(getString(R.string.dialog_location_page_number, cp.getPosition()+1, size));
											
											seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

												@Override
												public void onProgressChanged(
														SeekBar seekBar, int progress,
														boolean fromUser) {
													
													pageNum.setText(getString(R.string.dialog_location_page_number, progress+1, size));
													
												}

												@Override
												public void onStartTrackingTouch(
														SeekBar arg0) {
													// TODO Auto-generated method stub
													
												}

												@Override
												public void onStopTrackingTouch(
														SeekBar arg0) {
													// TODO Auto-generated method stub
													
												}
												
												
											});
											
											Dialog seekDialog = new Dialog(ViewComicActivity.this);
											AlertDialog.Builder seekBuilder = new AlertDialog.Builder(ViewComicActivity.this)
												.setTitle(R.string.dialog_open_location)
												.setView(contentView)
												.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
													
													public void onClick(DialogInterface dialog, int which) {
														dialog.cancel();													
													}
												}).setPositiveButton(R.string.dialog_open_location, new DialogInterface.OnClickListener() {
													
													public void onClick(DialogInterface dialog, int which) {
														dialog.cancel();
														
														cp.setCurrentItem(seeker.getProgress());
														
													}
												});
											
											seekDialog = seekBuilder.create();
											seekDialog.show();
											
										}
										
										break;
									case 3:
										cp.setCurrentItem(cp.getCount()-1);										
										break;
									}
									
									dialog.cancel();
								}
							});

			dialog = builder.create();
			dialog.show();
			
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			Log.d(TAG,
					"onOptionsItemSelected, unhandled id=" + item.getItemId());
			return super.onOptionsItemSelected(item);
		}
	}
	
}
