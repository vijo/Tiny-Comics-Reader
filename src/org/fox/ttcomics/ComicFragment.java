package org.fox.ttcomics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class ComicFragment extends Fragment {
	private final String TAG = this.getClass().getSimpleName();
	
	private SharedPreferences m_prefs;
	private int m_page;
	private CommonActivity m_activity;
	private File m_imageFile;
	
	public ComicFragment() {
		super();
	}
	
	public ComicFragment(int page) {
		super();
		m_page = page;
	}
	
	/* public Bitmap loadImage(ComicArchive archive, int page) {
		CommonActivity activity = (CommonActivity) getActivity();
		
		try {			
			final BitmapFactory.Options options = new BitmapFactory.Options();
		    options.inJustDecodeBounds = true;
		    BitmapFactory.decodeStream(archive.getItem(page), null, options);

	    	options.inSampleSize = CommonActivity.calculateInSampleSize(options, 512, 512);
		    options.inJustDecodeBounds = false;
		    
			return BitmapFactory.decodeStream(archive.getItem(page), null, options);
		} catch (OutOfMemoryError e) {
			if (activity != null) {		
				activity.toast(R.string.error_out_of_memory);
			}
			e.printStackTrace();
		} catch (IOException e) {
			if (activity != null) {
				activity.toast(R.string.error_loading_image);
			}
			e.printStackTrace();
		}
		
		return null;
	} */
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comic, container, false);
		
		final WebView web = (WebView) view.findViewById(R.id.comic);
		
		if (savedInstanceState != null) {
			m_page = savedInstanceState.getInt("page");
		}
		
		ComicPager pager = (ComicPager) getActivity().getSupportFragmentManager().findFragmentByTag(CommonActivity.FRAG_COMICS_PAGER);
		
		if (pager != null) {
			if (CommonActivity.isCompatMode() && m_prefs.getBoolean("use_dark_theme", false)) {
				web.setBackgroundColor(0xff000000);
			}
			
			WebSettings ws = web.getSettings();
			ws.setSupportZoom(true);
			ws.setBuiltInZoomControls(true);
			ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
			//ws.setUseWideViewPort(true);
			ws.setLoadWithOverviewMode(true);
			
		    // prevent flicker in ics
		    if (android.os.Build.VERSION.SDK_INT >= 11) {
		    	web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		    }
    	    
		    AsyncTask<InputStream, Void, File> loadTask = new AsyncTask<InputStream, Void, File>() {
				@Override
				protected File doInBackground(InputStream... params) {
					try {
				    	File imageFile = File.createTempFile("tcrview", ".jpg", getActivity().getExternalCacheDir());
				    	
						InputStream in = params[0];
						FileOutputStream out = new FileOutputStream(imageFile);
						
						int c;
						while ((c = in.read()) != -1) {
							out.write(c);
						}
						
						out.close();
						in.close();
						
						return imageFile;
						
					} catch (IOException e) {				
						e.printStackTrace();
					} catch (OutOfMemoryError e ) {
						e.printStackTrace();
					}
					
					return null;
				}
				
				@Override
				protected void onPostExecute(File result) {
					if (getActivity() != null && isAdded()) {
						if (result != null) {
							String url = "file://" + result.getAbsolutePath();
							
							String content = "<html>" +
									"<head>" +
									"<meta content=\"text/html; charset=utf-8\" http-equiv=\"content-type\">" +
									"<style type=\"text/css\">" +
									"body { padding : 0px; margin : 0px; background : transparent; }" +
									"</style>" +
									"</head>" +
									"<body><table width='100%' height='100%'>" +
									"<tr><td><img width='100%' src=\""+ url  +"\"></td></tr>" +
									"</table></body></html>";
						    
							Log.d(TAG, content);
							
							web.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
							
							m_imageFile = result;
						} else {
							((CommonActivity) getActivity()).toast(R.string.error_loading_image);
						}
					}
				}
		    	
		    };
		    
		    try {
				loadTask.execute(pager.getArchive().getItem(m_page));
			} catch (IOException e) {
				e.printStackTrace();
			} 
		    
			web.setOnTouchListener(new View.OnTouchListener() {
				int m_x;
				int m_y;
	
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						m_x = Math.round(event.getX());
						m_y = Math.round(event.getY());
						break;
					case MotionEvent.ACTION_UP:
						int x = Math.round(event.getX());
						int y = Math.round(event.getY());
						
						if (x == m_x && y == m_y) {
							int width = view.getWidth();
							
							if (x <= width/6) {
								onLeftSideTapped();
							} else if (x >= width-(width/6)) {
								onRightSideTapped();
							}
						}						
						break;
					}					
					return false;
				}
			});
			
			TextView page = (TextView) view.findViewById(R.id.comic_page);
			
			if (page != null) {
				page.setText(String.valueOf(m_page+1));
			}
		
		}

		return view;
		
	}
	
	private void onLeftSideTapped() {
		m_activity.selectPreviousComic();
	}

	private void onRightSideTapped() {
		m_activity.selectNextComic();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (m_imageFile != null) {
			m_imageFile.delete();
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		m_prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
		m_activity = (CommonActivity) activity;
	}
	
	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		out.putInt("page", m_page);
	}
	
}
