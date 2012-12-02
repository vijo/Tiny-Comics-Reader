package org.fox.ttcomics;

import java.io.IOException;
import java.util.HashMap;

import com.github.junrar.exception.RarException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ComicPager extends Fragment {
	private String m_fileName;
	private SharedPreferences m_prefs;
	private final String TAG = this.getClass().getSimpleName();
	private ComicArchive m_archive;
	private CommonActivity m_activity;
	
	private class PagerAdapter extends FragmentStatePagerAdapter {
		HashMap<Integer, ComicFragment> m_fragments = new HashMap<Integer, ComicFragment>();
		
		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		@Override
		public Fragment getItem(int position) {
			ComicFragment cf = new ComicFragment(position);			
			m_fragments.put(position, cf);			
			return cf;
		}
		
		@Override
		public int getCount() {
			return m_archive.getCount();
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
		    super.destroyItem(container, position, object);
		    m_fragments.remove(position);
		}		
		
		public ComicFragment getFragmentAt(int position) {
			return m_fragments.get(position);
		}
	}

	private PagerAdapter m_adapter;
	
	public ComicPager() {
		super();
	}
	
	public ComicArchive getArchive() {
		return m_archive;
	}
	
	public int getCount() {
		return m_adapter.getCount();
	}
	
	public int getPosition() {
		ViewPager pager = (ViewPager) getView().findViewById(R.id.comics_pager);
		
		if (pager != null) {
			return pager.getCurrentItem();
		}
		
		return 0;
	}
	
	public String getFileName() {
		return m_fileName;
	}
	
	public void setCurrentItem(int item) {
		ViewPager pager = (ViewPager) getView().findViewById(R.id.comics_pager);
		
		if (pager != null) {
			try {
				pager.setCurrentItem(item);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}
	
	public ComicPager(String fileName) {
		super();
		
		m_fileName = fileName;
	}
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		final View view = inflater.inflate(R.layout.fragment_comics_pager, container, false);
	
		m_adapter = new PagerAdapter(getActivity().getSupportFragmentManager());
		
		final ViewPager pager = (ViewPager) view.findViewById(R.id.comics_pager);
		
		if (savedInstanceState != null) {
			m_fileName = savedInstanceState.getString("fileName");
		}
		
		try {
			if (m_fileName.toLowerCase().matches(".*\\.(cbz|zip)")) {									
				m_archive = new CbzComicArchive(m_fileName);
			} else {
				m_archive = new CbrComicArchive(m_fileName);
			}
			
			int position = m_activity.getLastPosition(m_fileName);
			
			pager.setAdapter(m_adapter);
			pager.setCurrentItem(position);
			
			m_activity.onComicSelected(m_fileName, position);
			m_activity.setProgress(Math.round(((float)position / (float)(m_archive.getCount()-1)) * 10000));
			
		} catch (IOException e) {
			m_activity.toast(R.string.error_could_not_open_comic_archive);
			e.printStackTrace();
		} catch (RarException e) {
			m_activity.toast(R.string.error_could_not_open_comic_archive);
			e.printStackTrace();
		}
		
		pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			private ComicFragment m_current;
			
			public void onPageSelected(int position) {
				m_activity.onComicSelected(m_fileName, position);
				m_activity.setProgress(Math.round(((float)position / (float)(m_archive.getCount()-1)) * 10000));
								
				if (!CommonActivity.isCompatMode() && m_prefs.getBoolean("dim_status_bar", false)) {
					view.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
				}
				
				ComicFragment cf = m_adapter.getFragmentAt(position);

				if (cf != null) {
					if (m_current != null) {
						m_current.setThumbnail(true);
					}
					
					cf.setThumbnail(false);
					m_current = cf;
				}
			}
			
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		return view;		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		m_activity = (CommonActivity) activity;
		
		m_prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
	}
	
	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putString("fileName", m_fileName);
	}
	
}
