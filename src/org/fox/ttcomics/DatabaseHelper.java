package org.fox.ttcomics;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	public static final String DATABASE_NAME = "ComicsCache.db";
	public static final int DATABASE_VERSION = 2;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS comics_cache;");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS comics_cache (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                "filename TEXT, " +
                "path TEXT, " +				//v2
                "checksum TEXT, " +			//v2
                "size INTEGER, " +
                "position INTEGER, " +
                "max_position INTEGER" +
                ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			
			db.execSQL("ALTER TABLE comics_cache ADD COLUMN path TEXT;");
			db.execSQL("ALTER TABLE comics_cache ADD COLUMN checksum TEXT;");
			
			Cursor c = db.query("comics_cache", null,
					null, null, null, null, null);
			
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					int id = c.getInt(c.getColumnIndex(BaseColumns._ID));
					String fileName = c.getString(c.getColumnIndex("filename"));
	
					File file = new File(fileName);
					
					SQLiteStatement stmt = db.compileStatement("UPDATE comics_cache SET filename = ?, path = ? WHERE " + BaseColumns._ID + " = ?");
					stmt.bindString(1, file.getName());
					stmt.bindString(2, file.getParentFile().getAbsolutePath());
					stmt.bindLong(3, id);
					stmt.execute();
	
					c.moveToNext();
				}
			}
			
			c.close();
		}		
	}

}
