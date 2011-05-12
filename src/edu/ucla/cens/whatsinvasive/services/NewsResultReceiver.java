package edu.ucla.cens.whatsinvasive.services;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract.News;

public class NewsResultReceiver extends ResultReceiver {
    private static final String TAG = "NewsResultsReceiver";
    
    private final ContentResolver mContentResolver;
    private final SharedPreferences mPreferences;

    public NewsResultReceiver(Context context) {      
        super(null);
        
        this.mContentResolver = context.getContentResolver();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        switch(resultCode) {
        case DataService.SUCCESS:
            String result = resultData.getString("data");
            
            if(!TextUtils.isEmpty(result)) {
                try {
                    JSONArray results = new JSONArray(result);
                    
                    for(int i = 0; i < results.length(); i++) {
                        ArrayList<ContentValues> contentValues = new ArrayList<ContentValues>();
                        JSONObject catObj = results.getJSONObject(i);
                        JSONArray news = catObj.getJSONArray("news");
                        
                        for(int j = 0; j < news.length(); j++) {
                            ContentValues values = new ContentValues();
                            JSONObject obj = news.getJSONObject(j);
                            
                            values.put(News.NEWS_ID, obj.getLong("id"));
                            values.put(News.NEWS_TITLE, obj.getString("title"));
                            values.put(News.NEWS_AUTHOR, obj.getString("author"));
                            values.put(News.NEWS_CONTENT, obj.getString("content"));
                            values.put(News.NEWS_LINK, obj.getString("link"));
                            values.put(News.NEWS_PUBLISHED_TIME, obj.getLong("published_time"));
                            values.put(News.NEWS_CATEGORY, obj.getInt("category"));
                            
                            contentValues.add(values);
                        }
                        
                        mContentResolver.delete(News.CONTENT_URI, WhatsInvasiveProviderContract.News.NEWS_CATEGORY + "=?", 
                                new String[] { catObj.getString("id") });
                        
                        mContentResolver.bulkInsert(News.CONTENT_URI, contentValues.toArray(new ContentValues[] {}));
                    }
                    
                    mPreferences.edit().putLong("last_news_update", System.currentTimeMillis()/1000).commit();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }
            }
            break;
        default:
            break;
        }
    }

}
