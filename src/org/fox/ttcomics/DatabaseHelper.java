package org.fox.ttcomics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {

	private final String TAG = this.getClass().getSimpleName();
	
	public static final String DATABASE_NAME = "ComicsCache.db";
	public static final int DATABASE_VERSION = 1;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS comics_cache;");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS comics_cache (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                "filename TEXT, " +
                "size INTEGER, " +
                "position INTEGER, " +
                "max_position INTEGER" +
                ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//
	}

}
