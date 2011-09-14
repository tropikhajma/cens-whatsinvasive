package edu.ucla.cens.whatsinvasive;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import edu.ucla.cens.whatsinvasive.data.TagDatabase;
import edu.ucla.cens.whatsinvasive.services.LocationService;
import edu.ucla.cens.whatsinvasive.tools.Media;

public class TagLocation extends ListActivity {
    
    public static final String PREFERENCES_USER = "user";
    
    private TagDatabase mDatabase;
    private Cursor mCursor;
    
    private TagType mTagType;
    
    private long mParkId;

    private static final int MENU_HELP = 0;

    protected static final String TAG = "TagLocation";

    private static final int QUEUE_SCREEN = 323;

    private static final int SETTINGS_HELP = 182;
    
    private static final int DIALOG_TEST_LOGIN = 0;

    // Check user preferences
    SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_taglocation);
        setContentView(R.layout.tag_location);

        // Check user preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mTagType = (TagType)TagLocation.this.getIntent().getSerializableExtra("Type");
        
        if(mTagType == null) mTagType = TagType.WEED;
        
        Log.d(TAG, "Tag Type = " + mTagType.toString());

        // show tag help on first run
        if (!mPreferences.getBoolean("seen_tag_help", false)) {
            Log.d(TAG, "Showing Tag Help...");
            
            Intent help = new Intent(this, HelpImg.class);
            help.putExtra("help type", HelpImg.TAG_HELP);
            startActivity(help);
            mPreferences.edit().putBoolean("seen_tag_help", true).commit();
        }
        
        mDatabase = new TagDatabase(this);
                
        getListView().addHeaderView(getListHeader());
        getListView().setOnItemClickListener(new TagItemClickListener());
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Opening database");
        
        mDatabase.openRead();
        mParkId = LocationService.getParkId(this);
        mCursor = mDatabase.getTags(mParkId, mTagType);
        startManagingCursor(mCursor);
        setListAdapter(new TagAdapter(mCursor));

        final TextView parkName = (TextView)findViewById(R.id.park_name);
        parkName.setText(LocationService.getParkTitle(this, mParkId));
        
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Closing cursor and database");
        
        stopManagingCursor(mCursor);        
        mCursor.close();
        mDatabase.close();
        
        super.onPause();
    }

    private View getListHeader() {
        final float scale = getResources().getDisplayMetrics().density;
        
        LinearLayout layout = new LinearLayout(this);   
        layout.setLayoutParams(new ListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setBackgroundColor(getResources().getColor(R.color.dark_gray));
        layout.setClickable(false);
        layout.setLongClickable(false);
        
        TextView info = new TextView(this);
        info.setGravity(Gravity.CENTER);
        info.setWidth((int) ((105 * scale) + 0.5f));
        info.setText(R.string.header_tag_info);
        info.setTextSize(16);
        info.setTextColor(getResources().getColor(R.color.white));
        
        View divider = new View(this);
        divider.setBackgroundResource(R.drawable.divider_light);
        divider.setLayoutParams(new LinearLayout.LayoutParams(1, LayoutParams.FILL_PARENT));
        
        TextView observation = new TextView(this);
        observation.setGravity(Gravity.CENTER);
        observation.setText(R.string.header_tag_observation);
        observation.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
        observation.setTextSize(16);
        observation.setTextColor(getResources().getColor(R.color.white));
        
        layout.addView(info);
        layout.addView(divider);
        layout.addView(observation);
        
        return layout;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {        
        switch(id) {
        case DIALOG_TEST_LOGIN:
            return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.login_test_title))
                .setMessage(getString(R.string.login_test_msg))
                .setPositiveButton(getString(android.R.string.ok), null)
                .create();
        default:
            return super.onCreateDialog(id);
        
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_HELP, 1, getString(R.string.help_tagging_title))
                .setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, QUEUE_SCREEN, 2, getString(R.string.menu_queue)).setIcon(
                android.R.drawable.ic_menu_sort_by_size);
        menu.add(0, SETTINGS_HELP, 3, getString(R.string.menu_settings))
                .setIcon(android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;

        switch (item.getItemId()) {
        case MENU_HELP:
            intent = new Intent(this, HelpImg.class);
            intent.putExtra("help type", HelpImg.TAG_HELP);
            startActivity(intent);
            break;
        case QUEUE_SCREEN:
            intent = new Intent(this, Queue.class);
            startActivity(intent);
            break;
        case SETTINGS_HELP:
            intent = new Intent(this, Settings.class);
            startActivity(intent);
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private class TagItemClickListener implements OnItemClickListener
    {
        public void onItemClick(AdapterView<?> arg0, View v, int arg2,
                long arg3) {
            Tag data = (Tag) v.getTag();
            
            if(data == null)
                return;
            
            String username = mPreferences.getString("username", "");
            String password = mPreferences.getString("password", "");
            
            if(username.equals("test") && password.equals("test") 
                    && LocationService.getParkId(TagLocation.this) != TagDatabase.DEMO_PARK_ID) {
                Log.i(TAG, "Cannot tag observations for non-DEMO parks while in demo mode");
                TagLocation.this.showDialog(DIALOG_TEST_LOGIN);
                return;
            }

            // Tag this invasive
            final Intent intent = new Intent(TagLocation.this, TagTasks.class);
            intent.putExtra("type", mTagType);
            intent.putExtra("park_id", mParkId);
            intent.putExtra("id", data.id);
            intent.putExtra("common_name", data.title);
            intent.putExtra("science_name", data.scienceName);
            TagLocation.this.startActivity(intent);
        }
    }

    private class Tag {
        public int id;

        public String title;
        
        public String scienceName;

        public String imagePath;

        public String flags;

        public String info;
    }
    
    private class TagAdapter extends ResourceCursorAdapter {

        private boolean mLoading;

        public TagAdapter(Cursor c) {
            super(TagLocation.this, R.layout.tag_list_item, c);
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }
        
        private Tag getTag(Cursor c) {
            Tag tag = new Tag();

            tag.id = c.getInt(c.getColumnIndex(TagDatabase.KEY_ID));
            tag.title = c.getString(c.getColumnIndex(TagDatabase.KEY_TITLE));
            tag.scienceName = c.getString(c.getColumnIndex(TagDatabase.KEY_SCIENCE_NAME));
            tag.info = c.getString(c.getColumnIndex(TagDatabase.KEY_TEXT));
            tag.imagePath = c.getString(c.getColumnIndex(TagDatabase.KEY_IMAGE_URL));
            tag.flags = c.getString(c.getColumnIndex(TagDatabase.KEY_FLAGS));

            return tag;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            
            Tag tag = getTag(c);

            TextView title = (TextView) view.findViewById(R.id.title);
            TextView scienceName = (TextView) view.findViewById(R.id.science_name);
            final ImageView image = (ImageView) view.findViewById(R.id.image);
            
            // set item title text

            title.setText(tag.title);
            scienceName.setText(tag.scienceName);

            if (tag.flags != null && tag.flags.contains("invasiveofweek")) {
                view.setBackgroundColor(Color.GREEN);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }

            if (tag.imagePath != null && new File(tag.imagePath).exists()) {
                Bitmap thumb = Media.resizeImage(tag.imagePath, new Media.Size(56, 56));

                //info.setPadding(thumb.getWidth() - 25, thumb.getHeight() - 40, 0, 0);

                image.setImageBitmap(thumb);
                image.setPadding(1, 1, 1, 1);
            } else {
                image.setImageResource(R.drawable.downloading);
                image.setPadding(1, 1, 1, 1);
            }
            
            View thumbnail = view.findViewById(R.id.Thumbnail);
            thumbnail.setTag(tag);
            thumbnail.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Tag data = (Tag) v.getTag();

                    Intent intent = new Intent(TagLocation.this,
                            TagHelp.class);
                    intent.putExtra("id", data.id);
                    intent.putExtra("type", TagLocation.this.mTagType);

                    TagLocation.this.startActivity(intent);
                }
            });

            view.setTag(tag);
        }
    }
}
