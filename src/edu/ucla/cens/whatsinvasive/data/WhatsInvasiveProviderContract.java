package edu.ucla.cens.whatsinvasive.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class WhatsInvasiveProviderContract {

    public interface NewsColumns {
        String NEWS_ID = "news_id";
        String NEWS_TITLE = "news_title";
        String NEWS_AUTHOR = "news_author";
        String NEWS_CONTENT = "news_content";
        String NEWS_LINK = "news_link";
        String NEWS_PUBLISHED_TIME = "news_published";
        String NEWS_CATEGORY = "news_category";
    }
    
//    public interface TagColumns {
//        
//    }
//    
//    public interface AreaColumns {
//        
//    }
//    
//    public interface ObservationColumns {
//        
//    }
    
    public static final String API_HOST = "sm.whatsinvasive.com";
    
    public static final String CONTENT_AUTHORITY = "edu.ucla.cens.whatsinvasive";
    
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    
    public static class News implements NewsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("news").build();
        
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.whatsinvasive.news";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.whatsinvasive.news";
        
        public static final String DEFAULT_SORT = NewsColumns.NEWS_PUBLISHED_TIME + " DESC";
        
        public static final String API_PATH = "phone/news.php";
        
        public static enum Category {
            INVASIVE_PLANTS(1),
            INVASIVE_ANIMALS(2),
            CONSERVATION(3);
            
            private final int id;
            
            private Category(int categoryId) {
                this.id = categoryId;
            }
            
            public int value() {
                return id;
            }
            
            public static Integer[] idValues() {
                Category[] values = Category.values();
                Integer[] idValues = new Integer[values.length];
                for(int i = 0; i < values.length; i++) {
                    idValues[i] = values[i].value();
                }
                
                return idValues;
            }
        }
    
        public static Uri buildNewsUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
        
        public static Uri buildCategoryUri(int categoryId) {
            return CONTENT_URI.buildUpon().appendPath("category")
                                          .appendPath(String.valueOf(categoryId)).build();
        }
        
        static long getNewsId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
        
        static int getCategory(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }
    }
    
    private WhatsInvasiveProviderContract() {}
}
