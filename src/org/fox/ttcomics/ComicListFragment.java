package org.fox.ttcomics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.junrar.exception.RarException;

public class ComicListFragment extends Fragment implements OnItemClickListener {
	private final String TAG = this.getClass().getSimpleName();

	private final static int SIZE_DIR = -100;
	
	// corresponds to tab indexes
	private final static int MODE_ALL = 0;
	private final static int MODE_UNREAD = 1;
	private final static int MODE_UNFINISHED = 2;
	private final static int MODE_READ = 3;
	
	private CommonActivity m_activity;
	private SharedPreferences m_prefs;
	private ComicsListAdapter m_adapter;
	private int m_mode = 0;
	private String m_baseDirectory = "";

	public ComicListFragment() {
		super();
	}

	public void setBaseDirectory(String baseDirectory) {
		m_baseDirectory = baseDirectory;		
	}
	
	public void setMode(int mode) {
		m_mode = mode;
	}
	
	private class ComicsListAdapter extends SimpleCursorAdapter {
		public ComicsListAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			Cursor c = (Cursor) getItem(position);
			
			String filePath = c.getString(c.getColumnIndex("path"));
			String fileBaseName = c.getString(c.getColumnIndex("filename"));
			String firstChild = c.getString(c.getColumnIndex("firstchild"));

			int lastPos = m_activity.getLastPosition(filePath + "/" + fileBaseName);
			int size = m_activity.getSize(filePath + "/" + fileBaseName);

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
				if (size != -1 && size != SIZE_DIR) {
					if (lastPos == size - 1) {
						info.setText(getString(R.string.file_finished));
					} else if (lastPos > 0) {
						info.setText(getString(R.string.file_progress_info, lastPos+1, size, (int)(lastPos/ (float)size * 100f)));						
					} else {
						info.setText(getString(R.string.file_unread, size));
					}
					info.setVisibility(View.VISIBLE);
				} else {
					info.setVisibility(View.GONE);
				}
			}
			
			ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.file_progress_bar);
			
			if (progressBar != null) {
				if (size != -1 && size != SIZE_DIR) {
					progressBar.setMax(size-1);
					progressBar.setProgress(lastPos);
					progressBar.setVisibility(View.VISIBLE);
				} else {
					progressBar.setVisibility(View.GONE);
				}
			}
			
			File thumbnailFile = new File(m_activity.getCacheFileName(firstChild != null ? firstChild :  filePath + "/" + fileBaseName));
			
			ImageView thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
			
			if (thumbnail != null) {
				if (size == SIZE_DIR) {
					thumbnail.setBackgroundResource(R.drawable.border_folder);
				} else {
					thumbnail.setBackgroundResource(R.drawable.border);
				}
				
				thumbnail.setImageResource(R.drawable.ic_launcher);
				
				if (m_activity.isStorageAvailable() && thumbnailFile.exists()) {
					thumbnail.setTag(thumbnailFile.getAbsolutePath());

					CoverImageLoader imageLoader = new CoverImageLoader();
					imageLoader.execute(thumbnail);
				}
			}

			return v;
		}
	}

	class CoverImageLoader extends AsyncTask<ImageView, Void, Bitmap> {
		private ImageView m_thumbnail;
		
		@Override
		protected Bitmap doInBackground(ImageView... params) {
			m_thumbnail = params[0];
			
			if (m_thumbnail != null) {
				File thumbnailFile = new File(m_thumbnail.getTag().toString());
				
				if (thumbnailFile.exists() && thumbnailFile.canRead()) {
				
				    final BitmapFactory.Options options = new BitmapFactory.Options();
				    options.inJustDecodeBounds = true;
				    BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath(), options);
		
				    options.inSampleSize = CommonActivity.calculateInSampleSize(options, 128, 128);
				    options.inJustDecodeBounds = false;
				    
				    Bitmap bmp = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath(), options);
				
				    return bmp;
				}
			}
			
			return null;
		}
		
		@Override
        protected void onPostExecute(Bitmap bmp) {
			if (isAdded() && bmp != null) {
				m_thumbnail.setImageBitmap(bmp);
			}
		}
		
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	
		
		View view = inflater.inflate(R.layout.fragment_comics_list, container, false);
		
		if (savedInstanceState != null) {
			m_mode = savedInstanceState.getInt("mode");
			m_baseDirectory = savedInstanceState.getString("baseDir");
			//m_files = savedInstanceState.getStringArrayList("files");
		}

       	m_adapter = new ComicsListAdapter(getActivity(), R.layout.comics_list_row, createCursor(), 
       			new String[] { "filename" }, new int[] { R.id.file_name }, 0);

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
		
		Cursor c = (Cursor) m_adapter.getItem(info.position);		
		String fileName = c.getString(c.getColumnIndex("path")) + "/" + c.getString(c.getColumnIndex("filename"));
		
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

	private Cursor createCursor() {
		String baseDir = m_baseDirectory.length() == 0 ? m_prefs.getString("comics_directory", "") : m_baseDirectory;
		
		String selection;
		String selectionArgs[];
		
		switch (m_mode) {
		case MODE_READ:
			selection = "path = ? AND position = size - 1";
			selectionArgs = new String[] { baseDir };
			break;
		case MODE_UNFINISHED:
			selection = "path = ? AND position < size AND position > 0 AND position != size - 1";
			selectionArgs = new String[] { baseDir };
			break;
		case MODE_UNREAD:
			selection = "path = ? AND position = 0 AND size != ?";
			selectionArgs = new String[] { baseDir, String.valueOf(SIZE_DIR) };
			break;
		default:
			selection = "path = ?";
			selectionArgs = new String[] { baseDir };
		}
		
		if (!m_prefs.getBoolean("enable_rar", false)) {
			selection += " AND (UPPER(filename) NOT LIKE '%.CBR' AND UPPER(filename) NOT LIKE '%.RAR')";
		}
		
		return m_activity.getReadableDb().query("comics_cache", new String[] { BaseColumns._ID, "filename", "path", 
					"(SELECT path || '/' || filename FROM comics_cache AS t2 WHERE t2.path = comics_cache.path || '/' || comics_cache.filename AND filename != '' ORDER BY filename LIMIT 1) AS firstchild" }, 
				selection, selectionArgs, null, null, "size != " + SIZE_DIR + ", filename, size = " + SIZE_DIR + ", filename");
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

    			int fileIndex = 0;

	    		if (dir.isDirectory()) {
	    			File archives[] = dir.listFiles();
	    			
	    			java.util.Arrays.sort(archives);
	    			
	    			for (File archive : archives) {
	    				String filePath = archive.getAbsolutePath();
	    				
	    				if (archive.isDirectory()) {
	    					m_activity.setSize(filePath, SIZE_DIR);
	    					
	    				} else if (archive.getName().toLowerCase().matches(".*\\.(cbz|zip|cbr|rar)") && isAdded() && m_activity != null && 
	    						m_activity.getWritableDb() != null && m_activity.getWritableDb().isOpen()) {
	    					try {
	    						int size = m_activity.getSize(filePath);
	    						
	    						if (size == -1 || fullRescan) {
	    						
									ComicArchive cba = null;
									
									if (archive.getName().toLowerCase().matches(".*\\.(cbz|zip)")) {									
										cba = new CbzComicArchive(filePath);
									} else {
										try {
											cba = new CbrComicArchive(filePath);
										} catch (RarException e) {
											e.printStackTrace();											
										}
									}
									
									if (cba != null && cba.getCount() > 0) {
										// Get cover
										
										try {
											File thumbnailFile = new File(m_activity.getCacheFileName(filePath));

											if (m_activity.isStorageWritable() && (!thumbnailFile.exists() || fullRescan)) {
												InputStream is = cba.getItem(0);

												if (is != null) {
													FileOutputStream fos = new FileOutputStream(thumbnailFile);							
													
													byte[] buffer = new byte[1024];
													int len = 0;
													while ((len = is.read(buffer)) != -1) {
													    fos.write(buffer, 0, len);
													}
													
													fos.close();
													is.close();
												}
											}

										} catch (IOException e) {
											e.printStackTrace();
										}
										
										size = cba.getCount();
										
										m_activity.setSize(filePath, size);
									}
	    						}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								e.printStackTrace();
							}
	    				}
	    				
	    				++fileIndex;
						
						publishProgress(Integer.valueOf(fileIndex), Integer.valueOf(archives.length));
	    			}
	    		}
				
	    		if (isAdded() && m_activity != null) {
	    			m_activity.cleanupCache(false);
	    			m_activity.cleanupSqliteCache(comicsDir);
	    		}
	    		
				return fileIndex; //m_files.size();
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if (isAdded() && m_adapter != null) {
					m_adapter.changeCursor(createCursor());
					m_adapter.notifyDataSetChanged();
				}
			}
		};
	
		String comicsDir = m_prefs.getString("comics_directory", null);
		
		if (comicsDir != null && m_activity.isStorageAvailable()) {
			rescanTask.execute(m_baseDirectory.length() > 0 ? m_baseDirectory : comicsDir);
		}
	}
	
    @Override
    public void onResume() {
    	super.onResume();
   	
    	m_adapter.notifyDataSetChanged();
    	
    	String comicsDir = m_prefs.getString("comics_directory", "");
    	
    	if (m_activity.getCachedItemCount(m_baseDirectory.length() > 0 ? m_baseDirectory : comicsDir) == 0) {
    		rescan(false);
    	} else {
    		m_adapter.notifyDataSetChanged();
    	} 
    }
	
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		//Log.d(TAG, "onItemClick position=" + position);
		
		Cursor c = (Cursor) m_adapter.getItem(position);
		String fileName = c.getString(c.getColumnIndex("path")) + "/" + c.getString(c.getColumnIndex("filename"));
		
		if (fileName != null) {
			m_activity.onComicArchiveSelected(fileName);
		}
	}


	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putInt("mode", m_mode);
		out.putString("baseDir", m_baseDirectory);
		//out.putStringArrayList("files", m_files);
	}
	
}
