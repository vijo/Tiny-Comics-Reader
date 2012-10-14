package org.fox.ttcomics;

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
	private Bitmap m_comic;
	private int m_page;
	
	public ComicFragment() {
		super();
	}
	
	public ComicFragment(InputStream is, int page) {
		super();
		m_comic = BitmapFactory.decodeStream(is);
		m_page = page;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comic, container, false);
		
		TouchImageView image = (TouchImageView) view.findViewById(R.id.comic_image);
		
		if (savedInstanceState != null) {
			m_comic = savedInstanceState.getParcelable("comic");
			m_page = savedInstanceState.getInt("page");
		}
		
		if (m_comic != null) {
			image.setImageBitmap(m_comic);
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

		out.putParcelable("comic", m_comic);
		out.putInt("page", m_page);
	}
	
}
