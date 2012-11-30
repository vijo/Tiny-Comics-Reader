package org.fox.ttcomics;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class ComicFragment extends Fragment {
	private final String TAG = this.getClass().getSimpleName();
	
	private SharedPreferences m_prefs;
	private int m_page;
	private CommonActivity m_activity;
	
	public ComicFragment() {
		super();
	}
	
	public ComicFragment(int page) {
		super();
		m_page = page;
	}
	
	public Bitmap loadImage(ComicArchive archive, int page) {
		CommonActivity activity = (CommonActivity) getActivity();
		
		try {			
			final BitmapFactory.Options options = new BitmapFactory.Options();
		    options.inJustDecodeBounds = true;
		    BitmapFactory.decodeStream(archive.getItem(page), null, options);

		    if (CommonActivity.isCompatMode()) {
		    	options.inSampleSize = CommonActivity.calculateInSampleSize(options, 512, 512);
		    } else {		    
		    	options.inSampleSize = CommonActivity.calculateInSampleSize(options, 1024, 1024);
		    }
		    
		    options.inJustDecodeBounds = false;
		    
			return BitmapFactory.decodeStream(archive.getItem(page), null, options);
		} catch (OutOfMemoryError e) {
			if (activity != null) {		
				activity.toast(R.string.error_out_of_memory);
			}
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comic, container, false);
		
		final ImageViewTouch image = (ImageViewTouch) view.findViewById(R.id.comic_image);
		
		if (savedInstanceState != null) {
			m_page = savedInstanceState.getInt("page");
		}
		
		ComicPager pager = (ComicPager) getActivity().getSupportFragmentManager().findFragmentByTag(CommonActivity.FRAG_COMICS_PAGER);
		
		if (pager != null) {
			if (CommonActivity.isCompatMode() && m_prefs.getBoolean("use_dark_theme", false)) {
				image.setBackgroundColor(0xff000000);
			}
			
			image.setFitToScreen(true);
			
			AsyncTask<ComicArchive, Void, Bitmap> loadTask = new AsyncTask<ComicArchive, Void, Bitmap>() {
				@Override
				protected Bitmap doInBackground(ComicArchive... params) {
					return loadImage(params[0], m_page);
				}
				
				@Override
				protected void onPostExecute(Bitmap result) {
					CommonActivity activity = (CommonActivity) getActivity();
					
					if (activity != null && isAdded()) {
						if (result != null) {
							image.setImageBitmap(result);
						} else {							
							activity.toast(R.string.error_loading_image);
							image.setImageResource(R.drawable.badimage);
						}
					}					
				}
			};
			
			loadTask.execute(pager.getArchive());
			
			image.setOnScaleChangedListener(new ImageViewTouch.OnScaleChangedListener() {
				@Override
				public void onScaleChanged(float scale) {
					/* ViewPager pager = (ViewPager) getActivity().findViewById(R.id.comics_pager);
					
					if (pager != null) {
						pager.setPagingEnabled(widthFits);
					} */
				}
			}); 

			image.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_UP:
						int x = Math.round(event.getX());
						//int y = Math.round(event.getY());
						
						int width = view.getWidth();
							
						if (x <= width/10) {
							onLeftSideTapped();
						} else if (x >= width-(width/10)) {
							onRightSideTapped();
						}
						break;
					}					
					return false;
				}
			}); 
			
		} 
		
		TextView page = (TextView) view.findViewById(R.id.comic_page);
		
		if (page != null) {
			page.setText(String.valueOf(m_page+1));
		}

		return view;
		
	}
	
	private void onLeftSideTapped() {
		ImageViewTouch image = (ImageViewTouch) getView().findViewById(R.id.comic_image);
		
		if (image != null) {
			boolean atLeftEdge = !image.canScroll(1);
			
			if (atLeftEdge) {
				m_activity.selectPreviousComic();
			}
		}
	}
	
	public boolean canScroll(int direction) {
		ImageViewTouch image = (ImageViewTouch) getView().findViewById(R.id.comic_image);
		
		return image.canScroll(direction);
	}
	
	private void onRightSideTapped() {
		ImageViewTouch image = (ImageViewTouch) getView().findViewById(R.id.comic_image);
		
		if (image != null) {
			boolean atRightEdge = !image.canScroll(-1);
			
			if (atRightEdge) {
				m_activity.selectNextComic();
			}
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
