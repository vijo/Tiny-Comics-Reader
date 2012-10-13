package org.fox.ttcomics;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.NumberPicker;

public class ViewComicActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();

	private String m_fileName;
	
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
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(m_fileName);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_view_comic, menu);
        return true;
    }
    
    /* public void onComicSelected(String fileName, int position, int size) {
    	super.onComicSelected(fileName, position, size);
    } */
    
	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putString("fileName", m_fileName);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_go_location:
			
			Dialog dialog = new Dialog(ViewComicActivity.this);
			AlertDialog.Builder builder = new AlertDialog.Builder(ViewComicActivity.this)
					.setTitle("Go to...")
					.setItems(
							new String[] {
									"Beginning",
									"Furthest read location",
									"Location...",
									"End" 
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
											.setTitle("Go to location")
											.setView(contentView)
											.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
												
												public void onClick(DialogInterface dialog, int which) {
													dialog.cancel();													
												}
											}).setPositiveButton("Go to location", new DialogInterface.OnClickListener() {
												
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

			
			// TODO display dialog: Beginning, Page..., Last unread			
			
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
