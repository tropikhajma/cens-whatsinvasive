package edu.ucla.cens.whatsinvasive.data;

import org.apache.commons.lang.StringUtils;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveDatabase.Tables;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract.News;
import edu.ucla.cens.whatsinvasive.services.DataService;
import edu.ucla.cens.whatsinvasive.services.NewsResultReceiver;

public class WhatsInvasiveProvider extends ContentProvider {
    private static final String TAG = "WhatsInvasiveProvider";
    
    private WhatsInvasiveDatabase mOpenHelper;
    private ContentResolver mContentResolver;
    
    private static final int AREAS = 10;
    
    private static final int TAGS = 20;
    
    private static final int OBSERVATIONS = 30;
    
    private static final int NEWS = 40;
    private static final int NEWS_CATEGORY = 41;
    private static final int NEWS_ID = 42;
    
    private static final int NEWS_LIMIT = 15;
    
    private static final UriMatcher sUriMatcher;
    
    static {
        final String authority = WhatsInvasiveProviderContract.CONTENT_AUTHORITY;
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        
        sUriMatcher.addURI(authority, "news", NEWS);
        sUriMatcher.addURI(authority, "news/category/*", NEWS_CATEGORY);
        sUriMatcher.addURI(authority, "news/*", NEWS_ID);
    }
    
    @Override
    public boolean onCreate() {
        mOpenHelper = new WhatsInvasiveDatabase(getContext());
        mContentResolver = getContext().getContentResolver();
        return true;
    }
    
    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)) {
        case NEWS:
            return News.CONTENT_TYPE;
        case NEWS_CATEGORY:
            return News.CONTENT_TYPE;
        case NEWS_ID:
            return News.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String defaultSort = null;
        Bundle serviceData = null;
        Bundle data = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean sync = false;
        
        switch(sUriMatcher.match(uri)) {
        case NEWS:
            qb.setTables(Tables.NEWS);
            defaultSort = News.DEFAULT_SORT;
            
            serviceData = new Bundle();
            serviceData.putParcelable("receiver", new NewsResultReceiver(getContext()));
            serviceData.putString("path", News.API_PATH);
            
            data = new Bundle();
            data.putString("categories", StringUtils.join(News.Category.idValues(), ','));
            data.putInt("limit", NEWS_LIMIT);
            data.putLong("last_update", preferences.getLong("last_news_update", 0));
            serviceData.putBundle("data", data);
            break;
        case NEWS_CATEGORY:
            qb.setTables(Tables.NEWS);
            qb.appendWhere(News.NEWS_CATEGORY + "=" + News.getCategory(uri));
            defaultSort = News.DEFAULT_SORT;
            
            serviceData = new Bundle();
            serviceData.putParcelable("receiver", new NewsResultReceiver(getContext()));
            serviceData.putString("path", News.API_PATH);
            
            data = new Bundle();
            data.putInt("categories", News.getCategory(uri));
            data.putInt("limit", NEWS_LIMIT);
            data.putLong("last_update", preferences.getLong("last_news_update", 0));
            serviceData.putBundle("data", data);
            break;
        case NEWS_ID:
            qb.setTables(Tables.NEWS);
            qb.appendWhere(News._ID + "=" + News.getNewsId(uri));
            defaultSort = News.DEFAULT_SORT;
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        
        if(TextUtils.isEmpty(sortOrder)) {
            sortOrder = defaultSort;
        }
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(mContentResolver, uri);
        
        if(serviceData != null) {
            Intent intent = new Intent(getContext(), DataService.class);
            intent.putExtras(serviceData);
            getContext().startService(intent);
        }
        
        return c;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table;
        String nullColumnHack = null;
        Uri contentUri = null;
        
        switch(sUriMatcher.match(uri)) {
        case NEWS:
            table = Tables.NEWS;
            nullColumnHack = News.NEWS_TITLE;
            contentUri = News.CONTENT_URI;
            
            if(!values.containsKey(News.NEWS_ID)) {
                throw new SQLException("News ID is required");
            }
            
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(table, nullColumnHack, values);
        
        
        if(rowId > 0) {
            Uri insertUri = ContentUris.withAppendedId(contentUri, rowId);
            mContentResolver.notifyChange(insertUri, null);
            
            return insertUri;
        }
        
        throw new SQLException("Failed to insert at " + uri);
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        String table;
        String where = null;
        
        switch(sUriMatcher.match(uri)) {
        case NEWS:
            table = Tables.NEWS;
            break;
        case NEWS_ID:
            table = Tables.NEWS;
            where = News._ID + "=" + News.getNewsId(uri);
            break;
        default:
           throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        
        if(where != null) {
            if(TextUtils.isEmpty(selection)) {
                selection = where;
            } else {
                selection = where + " AND (" + selection + ")";
            }
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(table, values, selection, selectionArgs);
        
        mContentResolver.notifyChange(uri, null);
        
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table;
        String where = null;
        
        switch(sUriMatcher.match(uri)) {
        case NEWS:
            table = Tables.NEWS;
            break;
        case NEWS_ID:
            table = Tables.NEWS;
            where = News._ID + "=" + News.getNewsId(uri);
            break;
        default:
           throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        
        if(where != null) {
            if(TextUtils.isEmpty(selection)) {
                selection = where;
            } else {
                selection = where + " AND (" + selection + ")";
            }
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(table, selection, selectionArgs);
        
        mContentResolver.notifyChange(uri, null);
        
        return count;
    }

}
