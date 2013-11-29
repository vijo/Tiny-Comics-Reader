package org.fox.ttcomics;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class SyncClient {
	public interface PositionReceivedListener {
		void onPositionReceived(int position);
	}
	
	private class HttpTask extends AsyncTask<String, Integer, Boolean> {
		protected String m_response = null;
		protected int m_responseCode = -1;
		
		@Override
		protected Boolean doInBackground(String... params) {
						
			String requestStr = null;
			String op = params[0];
			
			if (op.equals("set")) {
				requestStr = String.format("op=set&owner=%1$s&hash=%2$s&position=%3$s", m_owner, params[1], params[2]);
			} else if (op.equals("get")) {			 
				requestStr = String.format("op=get&owner=%1$s&hash=%2$s", m_owner, params[1]);
			} else if (op.equals("clear")) {
				if (params.length > 1) {
					requestStr = String.format("op=clear&owner=%1$s&hash=%2$s", m_owner, params[1]);
				} else {
					requestStr = String.format("op=clear&owner=%1$s", m_owner);
				}
			}
			
			Log.d(TAG, requestStr);
			
			if (requestStr == null) return false;
			
			try {
				byte[] postData = requestStr.getBytes("UTF-8");
				
				URL url = new URL(SYNC_ENDPOINT);
				
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true); 
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST"); 
				
			    OutputStream out = conn.getOutputStream();
			    out.write(postData);
			    out.close();

			    m_responseCode = conn.getResponseCode();				    

			    if (m_responseCode == HttpURLConnection.HTTP_OK) {
			    	StringBuffer response = new StringBuffer();
			    	InputStreamReader in = new InputStreamReader(conn.getInputStream(), "UTF-8");
			    	
					char[] buf = new char[1024];
					int read = 0;
					
					while ((read = in.read(buf)) >= 0) {
						response.append(buf, 0, read);
					}
					
					//Log.d(TAG, "<<< " + response);

					m_response = response.toString();

					if (response.indexOf("ERROR") == -1) {
						return true;
					} else {
						return false;
					}
			    } else {
			    	Log.d(TAG, "HTTP error, code: " + m_responseCode);
			    }
			    
			    conn.disconnect();
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return false;
		}
		
	}
	
	private final String TAG = this.getClass().getSimpleName();
	private static final String SYNC_ENDPOINT = "http://tt-rss.org/tcrsync/";
	private String m_owner = null;
	
	public void setOwner(String owner) {
		m_owner = CommonActivity.sha1(owner);
	}
	
	public int getPosition(String hash, final PositionReceivedListener listener) {
		if (m_owner != null) {
			Log.d(TAG, "Requesting sync data...");

			HttpTask task = new HttpTask() {
				@Override
				protected void onPostExecute(Boolean result) {
					if (result) {
						try {
							listener.onPositionReceived(Integer.valueOf(m_response));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			};
			
			task.execute("get", hash);

		}
		return -1;
	}
	
	public void setPosition(String hash, int position) {
		if (m_owner != null) {
			Log.d(TAG, "Uploading sync data...");
			
			HttpTask task = new HttpTask();
			
			task.execute("set", hash, String.valueOf(position));
		}
	}
	
	public void clearData() {
		if (m_owner != null) {
			Log.d(TAG, "Clearing sync data...");
			
			HttpTask task = new HttpTask();
			
			task.execute("clear");
		}
	}

	public void clearData(String hash) {
		if (m_owner != null) {
			Log.d(TAG, "Clearing sync data: " + hash);
			
			HttpTask task = new HttpTask();
			
			task.execute("clear", hash);
		}
	}

	public boolean hasOwner() {
		return m_owner != null;
	}
	
}
