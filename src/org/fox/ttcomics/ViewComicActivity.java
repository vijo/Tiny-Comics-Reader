package org.fox.ttcomics;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class ViewComicActivity extends CommonActivity {
	private final String TAG = this.getClass().getSimpleName();

	private String m_fileName;
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
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
