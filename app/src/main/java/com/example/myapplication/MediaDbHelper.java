package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MediaDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "media.db";

    private static final String TAG = "MEDIA_DB_HELPER_TAG";

    static final String SQL_CREATE_MEDIA_TABLE      = "CREATE TABLE media (id TEXT PRIMARY KEY,title TEXT,uri TEXT,coverImage TEXT,duration INTEGER,lastPosition INTEGER,genre TEXT)";
    static final String SQL_CREATE_TAGS_TABLE       = "CREATE TABLE tag (tag TEXT PRIMARY KEY)";
    static final String SQL_CREATE_MEDIA_TAGS_TABLE = "CREATE TABLE mediaTag (id INTEGER PRIMARY KEY AUTOINCREMENT,tag TEXT,media TEXT)";

    public MediaDbHelper (Context context)
    {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate.");
        db.execSQL( SQL_CREATE_MEDIA_TABLE );
        db.execSQL( SQL_CREATE_TAGS_TABLE );
        db.execSQL( SQL_CREATE_MEDIA_TAGS_TABLE );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL( "DROP TABLE IF EXISTS media" );
        db.execSQL( "DROP TABLE IF EXISTS tag" );
        db.execSQL( "DROP TABLE IF EXISTS mediaTag" );
        onCreate(db);
    }
}
