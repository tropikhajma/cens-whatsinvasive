package edu.ucla.cens.whatsinvasive;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract.News.Category;

public class News extends TabActivity {
    protected static final String TAG = "News";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);

        final Uri contentUri = WhatsInvasiveProviderContract.News.CONTENT_URI;
        final String where = WhatsInvasiveProviderContract.News.NEWS_CATEGORY + "=?";

        final Cursor plantsCursor = managedQuery(contentUri, null, where,
                new String[] { String.valueOf(Category.INVASIVE_PLANTS.value()) }, null);
        final Cursor animalsCursor = managedQuery(contentUri, null, where,
                new String[] { String.valueOf(Category.INVASIVE_ANIMALS.value()) }, null);
        final Cursor conservationCursor = managedQuery(contentUri, null, where,
                new String[] { String.valueOf(Category.CONSERVATION.value()) }, null);

        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("plants")
                .setIndicator("Invasive Plants")
                .setContent(R.id.invasive_plants));
        tabHost.addTab(tabHost.newTabSpec("animals")
                .setIndicator("Invasive Animals")
                .setContent(R.id.invasive_animals));
        tabHost.addTab(tabHost.newTabSpec("conservation")
                .setIndicator("Conservation")
                .setContent(R.id.conservation));

        this.fixTabs(tabHost.getTabWidget());

        final NewsItemClickListener listener = new NewsItemClickListener();
        final ListView plantListView = (ListView)findViewById(R.id.invasive_plants_list);
        plantListView.setOnItemClickListener(listener);
        plantListView.setAdapter(new NewsCursorAdapter(this, plantsCursor));
        plantListView.setEmptyView(findViewById(R.id.invasive_plants_empty));
        final ListView animalListView = (ListView)findViewById(R.id.invasive_animals_list);
        animalListView.setOnItemClickListener(listener);
        animalListView.setAdapter(new NewsCursorAdapter(this, animalsCursor));
        animalListView.setEmptyView(findViewById(R.id.invasive_animals_empty));
        final ListView conservationListView = (ListView)findViewById(R.id.conservation_list);
        conservationListView.setOnItemClickListener(listener);
        conservationListView.setAdapter(new NewsCursorAdapter(this, conservationCursor));
        conservationListView.setEmptyView(findViewById(R.id.conservation_empty));
    }

    private void fixTabs(TabWidget tabWidget) {
        RelativeLayout.LayoutParams params = 
            new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        int verticalMargin = (int) (2 * scale + 0.5f);
        int horizontalMargin = (int) (4 * scale + 0.5f);
        params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        
        for(int i = 0; i < tabWidget.getTabCount(); i++) {
            View tab = tabWidget.getChildTabViewAt(i);
            
            View icon = tab.findViewById(android.R.id.icon);
            icon.setVisibility(View.GONE);
            
            TextView title = (TextView)tab.findViewById(android.R.id.title);
            title.setLayoutParams(params);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(false);
        }
    }

    private class NewsTag {
        public long id;
        public String title;
        public String author;
        public String content;
        public String link;
        public long published;
    }

    private class NewsItemClickListener implements OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            NewsTag tag = (NewsTag)arg1.getTag();

            News.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(tag.link)));
        }
    }

    private class NewsCursorAdapter extends ResourceCursorAdapter {

        public NewsCursorAdapter(Context context, Cursor c) {
            super(context, R.layout.news_list_item, c, true);
        }

        private NewsTag getTag(Cursor c) {
            NewsTag tag = new NewsTag();
            
            tag.id = c.getLong(c.getColumnIndex(WhatsInvasiveProviderContract.News._ID));
            tag.title = c.getString(c.getColumnIndex(WhatsInvasiveProviderContract.News.NEWS_TITLE));
            tag.author = c.getString(c.getColumnIndex(WhatsInvasiveProviderContract.News.NEWS_AUTHOR));
            tag.content = c.getString(c.getColumnIndex(WhatsInvasiveProviderContract.News.NEWS_CONTENT));
            tag.link = c.getString(c.getColumnIndex(WhatsInvasiveProviderContract.News.NEWS_LINK));
            tag.published = c.getLong(c.getColumnIndex(WhatsInvasiveProviderContract.News.NEWS_PUBLISHED_TIME));

            return tag;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            NewsTag tag = getTag(cursor);
            
            TextView title = (TextView) view.findViewById(R.id.news_title);
            title.setText(Html.fromHtml(tag.title));
            
            TextView author = (TextView) view.findViewById(R.id.news_author);
            author.setText(tag.author);
            
            TextView content = (TextView) view.findViewById(R.id.news_content);
            content.setText(Html.fromHtml(tag.content));
            
            TextView date = (TextView) view.findViewById(R.id.news_date);
            date.setText(new SimpleDateFormat("yyyy-MM-dd h:mm:ss a").format(new Date(tag.published * 1000)));
            
            view.setTag(tag);
        }
    }
}
