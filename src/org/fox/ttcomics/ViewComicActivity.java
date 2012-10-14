package org.fox.ttcomics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.NumberPicker;

public class ViewComicActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();
	
	private final static int REQUEST_SHARE = 1;

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

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(new File(m_fileName).getName());
        
        if (m_prefs.getBoolean("use_full_screen", false)) {
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_view_comic, menu);
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
				
				startActivityForResult(Intent.createChooser(shareIntent, "Share comic"), REQUEST_SHARE);
								
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
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share:
			shareComic();			
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
