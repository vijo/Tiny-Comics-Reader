package org.fox.ttcomics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ComicListFragment extends Fragment implements OnItemClickListener {
	private final String TAG = this.getClass().getSimpleName();
	
	private CommonActivity m_activity;
	private SharedPreferences m_prefs;
	private ComicsListAdapter m_adapter;
	private ArrayList<String> m_files = new ArrayList<String>();
	private int m_mode = 0;
	private String m_baseDirectory = "";

	public ComicListFragment() {
		super();
	}

	public void setBaseDirectory(String baseDirectory) {
		m_baseDirectory = baseDirectory;		
	}
	
	public ComicListFragment(int mode) {
		super();
		
		m_mode = mode;
	}

	private class ComicsListAdapter extends ArrayAdapter<String> {
		private ArrayList<String> items;

		public ComicsListAdapter(Context context, int textViewResourceId, ArrayList<String> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			File file = new File(items.get(position));
			String fileBaseName = file.getName();

			int lastPos = m_activity.getLastPosition(file.getAbsolutePath());
			int size = m_activity.getSize(file.getAbsolutePath());

			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				
				v = vi.inflate(ComicListFragment.this.getView().findViewById(R.id.comics_list) != null ? R.layout.comics_list_row : R.layout.comics_grid_row, null);

			}
			
			TextView name = (TextView) v.findViewById(R.id.file_name);
			
			if (name != null) {
				name.setText(fileBaseName);
			}

			TextView info = (TextView) v.findViewById(R.id.file_progress_info);
			
			if (info != null) {
				if (size != -1) {
					info.setText(getString(R.string.file_progress_info, lastPos+1, size));
					info.setVisibility(View.VISIBLE);
				} else {
					info.setVisibility(View.GONE);
				}
			}
			
			ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.file_progress_bar);
			
			if (progressBar != null) {
				if (size != -1) {
					progressBar.setMax(size-1);
					progressBar.setProgress(lastPos);
					progressBar.setVisibility(View.VISIBLE);
				} else {
					progressBar.setVisibility(View.GONE);
				}
			}
			
			File thumbnailFile = new File(m_activity.getCacheFileName(file.getAbsolutePath()));
			
			ImageView thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
			
			if (thumbnail != null) {
				if (thumbnailFile.exists()) {
					//
					
				    final BitmapFactory.Options options = new BitmapFactory.Options();
				    options.inJustDecodeBounds = true;
				    BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath(), options);

				    options.inSampleSize = CommonActivity.calculateInSampleSize(options, 128, 128);
				    options.inJustDecodeBounds = false;
				    
				    Bitmap bmp = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath(), options);
				    
					if (bmp != null) {
						thumbnail.setImageBitmap(bmp);
					}
				} else {
					thumbnail.setImageResource(R.drawable.ic_launcher);
				}
			}

			return v;
		}
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comics_list, container, false);
		
		if (savedInstanceState != null) {
			m_mode = savedInstanceState.getInt("mode");
			m_files = savedInstanceState.getStringArrayList("files");
		}

       	m_adapter = new ComicsListAdapter(getActivity(), R.layout.comics_list_row, m_files);

		if (view.findViewById(R.id.comics_list) != null) {
			ListView list = (ListView) view.findViewById(R.id.comics_list);
			list.setAdapter(m_adapter);
			list.setEmptyView(view.findViewById(R.id.no_comics));
			list.setOnItemClickListener(this);
			
	       	registerForContextMenu(list);

		} else {
			GridView grid = (GridView) view.findViewById(R.id.comics_grid);
			grid.setAdapter(m_adapter);
			grid.setEmptyView(view.findViewById(R.id.no_comics));			
			grid.setOnItemClickListener(this);
						
	       	registerForContextMenu(grid);

		}
       	
		return view;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
		
		getActivity().getMenuInflater().inflate(R.menu.comic_archive_context, menu);

		// menu.setTitle(..); etc
		
		super.onCreateContextMenu(menu, v, menuInfo);		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		
		String fileName = m_adapter.getItem(info.position);
		
		switch (item.getItemId()) {
		case R.id.menu_reset_progress:
			if (fileName != null) {
				m_activity.setLastPosition(fileName, 0);
				m_activity.setLastMaxPosition(fileName, 0);
				m_adapter.notifyDataSetChanged();
			}
			return true;
		case R.id.menu_mark_as_read:
			
			if (fileName != null) {
				m_activity.setLastPosition(fileName, m_activity.getSize(fileName)-1);
				m_adapter.notifyDataSetChanged();
			}

			return true;		
		default:
			Log.d(TAG, "onContextItemSelected, unhandled id=" + item.getItemId());
			return super.onContextItemSelected(item);
		}
	}

	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		m_activity = (CommonActivity)activity;
		
		m_prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
	}
	
	protected void rescan(final boolean fullRescan) {

		AsyncTask<String, Integer, Integer> rescanTask = new AsyncTask<String, Integer, Integer>() {

			@Override
			protected void onProgressUpdate(Integer... progress) {
				if (isAdded()) {
					m_activity.setProgress(Math.round(((float)progress[0] / (float)progress[1]) * 10000));
				}
		    }
			
			@Override
			protected Integer doInBackground(String... params) {
		    	String comicsDir = params[0];

		    	File dir = new File(comicsDir);
		    		
	    		m_files.clear();
	    		
	    		if (dir.isDirectory()) {
	    			File archives[] = dir.listFiles();
	    			int fileIndex = 0;
	    			
	    			java.util.Arrays.sort(archives);
	    			
	    			for (File archive : archives) {
	    				String filePath = archive.getAbsolutePath();
	    				
	    				if (archive.isDirectory() && m_mode == 0) {
	    					m_files.add(filePath);
	    					
	    				} else if (archive.getName().toLowerCase().matches(".*\\.(cbz|zip)") && isAdded() && m_activity != null) {
	    					try {
	    						int size = m_activity.getSize(filePath);
	    						
	    						if (size == -1 || fullRescan) {
	    						
									CbzComicArchive cba = new CbzComicArchive(filePath);
									
									if (cba.getCount() > 0) {
										// Get cover
										
										try {
											InputStream is = cba.getItem(0);
										
											File thumbnailFile = new File(m_activity.getCacheFileName(filePath));

											if (!thumbnailFile.exists() || fullRescan) {
												FileOutputStream fos = new FileOutputStream(thumbnailFile);							
												
												byte[] buffer = new byte[1024];
												int len = 0;
												while ((len = is.read(buffer)) != -1) {
												    fos.write(buffer, 0, len);
												}
												
												fos.close();
												is.close();
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
										
										size = cba.getCount();
										
										m_activity.setSize(filePath, size);
									}
	    						}
									
								int lastPos = m_activity.getLastPosition(filePath); 
								
								switch (m_mode) {
								case 0:
									m_files.add(filePath);
									break;
								case 1:
									if (lastPos == 0) {
										m_files.add(filePath);
									}
									break;
								case 2:
									if (lastPos > 0 && lastPos != size - 1) {
										m_files.add(filePath);
									}
									break;
								case 3:
									if (lastPos == size - 1) {
										m_files.add(filePath);
									}
									break;								
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	
	    				}
	    				
	    				++fileIndex;
						
						publishProgress(Integer.valueOf(fileIndex), Integer.valueOf(archives.length));
	    			}
	    		}
				
	    		if (isAdded() && m_activity != null) {
	    			m_activity.cleanupCache(false);
	    		}
	    		
				return m_files.size();
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if (isAdded() && m_adapter != null) {
					m_adapter.notifyDataSetChanged();
				}
			}
		};
	
		String comicsDir = m_prefs.getString("comics_directory", null);
		
		if (comicsDir != null) {
			rescanTask.execute(m_baseDirectory.length() > 0 ? m_baseDirectory : comicsDir);
		}
	}
	
    @Override
    public void onResume() {
    	super.onResume();
   	
    	if (m_files.size() == 0) {
    		rescan(false);
    	} else {
    		m_adapter.notifyDataSetChanged();
    	}
    }
	
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		Log.d(TAG, "onItemClick position=" + position);
		
		String fileName = m_adapter.getItem(position);
		
		if (fileName != null) {
			m_activity.onComicArchiveSelected(fileName);
		}
	}


	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putInt("mode", m_mode);
		out.putStringArrayList("files", m_files);
	}
	
}
