package org.fox.ttcomics;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ComicFragment extends Fragment {
	private final String TAG = this.getClass().getSimpleName();
	
	private SharedPreferences m_prefs;
	private int m_page;
	
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
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comic, container, false);
		
		TouchImageView image = (TouchImageView) view.findViewById(R.id.comic_image);
		
		if (savedInstanceState != null) {
			m_page = savedInstanceState.getInt("page");
		}
		
		ComicPager pager = (ComicPager) getActivity().getSupportFragmentManager().findFragmentByTag(CommonActivity.FRAG_COMICS_PAGER);
		
		if (pager != null) {
			image.setImageBitmap(loadImage(pager.getArchive(), m_page));
			image.setMaxZoom(4f);
			image.setOnScaleChangedListener(new TouchImageView.OnScaleChangedListener() {
				
				public void onScaleChanged(float scale) {
					ViewPager pager = (ViewPager) getActivity().findViewById(R.id.comics_pager);
					
					if (pager != null) {
						pager.setPagingEnabled(scale - 1.0f < 0.01);
					}
				}
			});
		}
		
		TextView page = (TextView) view.findViewById(R.id.comic_page);
		
		if (page != null) {
			page.setText(String.valueOf(m_page));
		}
		
		return view;
		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		m_prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
	}
	
	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		out.putInt("page", m_page);
	}
	
}
