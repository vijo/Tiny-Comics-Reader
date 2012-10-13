package org.fox.ttcomics;

import java.io.InputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ComicFragment extends Fragment {
	private final String TAG = this.getClass().getSimpleName();
	
	private SharedPreferences m_prefs;
	private Bitmap m_comic;
	
	public ComicFragment() {
		super();
	}
	
	public ComicFragment(InputStream is) {
		super();
		m_comic = BitmapFactory.decodeStream(is);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comic, container, false);
		
		ImageView image = (ImageView) view.findViewById(R.id.comic_image);
		
		if (savedInstanceState != null) {
			m_comic = savedInstanceState.getParcelable("comic");
		}
		
		if (m_comic != null) {
			image.setImageBitmap(m_comic);
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
	}
	
}
