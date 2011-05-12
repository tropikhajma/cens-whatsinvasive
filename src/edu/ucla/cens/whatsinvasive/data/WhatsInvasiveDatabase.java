package edu.ucla.cens.whatsinvasive.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract.NewsColumns;

public class WhatsInvasiveDatabase extends SQLiteOpenHelper {
    private static final String TAG = "WhatsInvasiveDatabase";
    
    private static final String DATABASE_NAME = "whatsinvasive.db";
    private static final int DATABASE_VERSION = 1;
    
    interface Tables {
        String NEWS = "news";
//        String TAGS = "tags";
//        String OBSERVATIONS = "observations";
    }
    
    public WhatsInvasiveDatabase(Context ctx)
    {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.NEWS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NewsColumns.NEWS_ID + " INTEGER NOT NULL,"
                + NewsColumns.NEWS_TITLE + " TEXT,"
                + NewsColumns.NEWS_AUTHOR + " TEXT,"
                + NewsColumns.NEWS_CONTENT + " TEXT,"
                + NewsColumns.NEWS_LINK + " TEXT,"
                + NewsColumns.NEWS_PUBLISHED_TIME + " INTEGER,"
                + NewsColumns.NEWS_CATEGORY + " INTEGER,"
                + "UNIQUE (" + NewsColumns.NEWS_ID + ") ON CONFLICT REPLACE)");
        
//        db.execSQL("CREATE TABLE " + Tables.TAGS + " ("
//                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,");
//        
//        db.execSQL("CREATE TABLE " + Tables.OBSERVATIONS + " ("
//                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.NEWS);
//        db.execSQL("DROP TABLE IF EXISTS " + Tables.TAGS);
//        db.execSQL("DROP TABLE IF EXISTS " + Tables.OBSERVATIONS);
        onCreate(db);
    }
}
