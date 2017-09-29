package com.example.rangedownloadts.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

	public DbHelper(Context context, String name, int version) {
		super(context, name, null, version);
	}

	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table download_info(_id integer PRIMARY KEY AUTOINCREMENT, thread_id integer, "
				+ "start_pos integer, end_pos integer, compelete_size integer,url char)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
