package edu.ucla.cens.whatsinvasive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucla.cens.whatsinvasive.data.PhotoDatabase;
import edu.ucla.cens.whatsinvasive.data.TagDatabase;
import edu.ucla.cens.whatsinvasive.services.UploadService;

public class TagTasks extends Activity {
    protected static final String TAG = "TagTasks";
    
    private static final int ACTIVITY_CAPTURE_PHOTO = 0;
    private static final int ACTIVITY_CAPTURE_NOTE = 1;
    
    private static final int ACTIVITY_LOCATION_SETTINGS = 50;
    
    private static final int DIALOG_QUANTITY = 20;

    private static final int MAX_IMAGE_WIDTH = 1280;
    private static final int MAX_IMAGE_HEIGHT = 960;
    
    private View mPhotoTask;
    private View mNoteTask;
    private View mQuantityTask;
    private View mLocationTask;
    private Button mSubmitBtn;
    
    private long mParkId = -1;
    private String mCommonName = null;
    private String mScienceName = null;
    private String mNote = null;
    private int mAmountId = 0;
    private Location mLocation = null;
    private String mPhotoFilename = null;
    private TagType mTagType = null;
    
    protected SharedPreferences mPreferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_tasks);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        mAmountId = mPreferences.getInt("amount_id", 0);
        
        //  Setup the different tasks
        mPhotoTask = findViewById(R.id.tag_task_photo);
        setupTask(mPhotoTask, R.string.tag_tasks_title_photo, R.string.tag_tasks_description_photo,
                false, R.string.tag_tasks_btn_add, 
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        capturePhoto();
                    }
                });
        
        mNoteTask = findViewById(R.id.tag_task_note);
        setupTask(mNoteTask, R.string.tag_tasks_title_note, R.string.tag_tasks_description_note,
                false, R.string.tag_tasks_btn_add,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        captureNote();
                    }
                });
        
        mQuantityTask = findViewById(R.id.tag_task_quantity);
        setupTask(mQuantityTask, R.string.tag_tasks_title_quantity, R.string.tag_tasks_description_quantity,
                false, R.string.tag_tasks_btn_change,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        captureQuantity();
                    }
                });
        
        mLocationTask = findViewById(R.id.tag_task_location);
        setupTask(mLocationTask, R.string.tag_tasks_title_location, R.string.tag_tasks_description_location,
                false, R.string.tag_tasks_btn_change,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
                    }
                });
        
        mSubmitBtn = (Button)findViewById(R.id.submit_btn);
        mSubmitBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recordObservation();
                    }
                });
        
        if(savedInstanceState != null) {
            mAmountId = savedInstanceState.getInt("amount_id", mAmountId);
            mTagType = (TagType)savedInstanceState.getSerializable("type");
            mParkId = savedInstanceState.getLong("park_id", TagDatabase.DEMO_PARK_ID);
            mCommonName = savedInstanceState.getString("common_name");
            mScienceName = savedInstanceState.getString("science_name");
            mLocation = (Location)savedInstanceState.getParcelable("location");
            
            mNote = savedInstanceState.getString("note");
            if(mNote != null) {
                setChecked(mNoteTask, true);
                setDescription(mNoteTask, mNote);
                setButtonText(mNoteTask, R.string.tag_tasks_btn_change);
            }
            
            mPhotoFilename = savedInstanceState.getString("filename");
            if(mPhotoFilename != null) {
                setChecked(mPhotoTask, true);
                setDescription(mPhotoTask, getString(R.string.tag_tasks_description_photo_replace));
                setButtonText(mPhotoTask, R.string.tag_tasks_btn_change);
            }
                
            if(savedInstanceState.getBoolean("quantity_checked")) {
                setChecked(mQuantityTask, true);
            }
        } else {
            mTagType = (TagType)getIntent().getSerializableExtra("type");
            mParkId = getIntent().getLongExtra("park_id", TagDatabase.DEMO_PARK_ID);
            mCommonName = getIntent().getStringExtra("common_name");
            mScienceName = getIntent().getStringExtra("science_name");
        }
        
        // Set the header information
        final TextView commonName = (TextView)findViewById(R.id.common_name);
        commonName.setText(mCommonName);
        
        final TextView scienceName = (TextView)findViewById(R.id.science_name);
        scienceName.setText(mScienceName);
        
        updateQuantityTask();
        updateLocationTask();
    }

    protected void setupTask(View taskView, int titleResId, int descriptionResId, 
            boolean checked, int buttonTextResId, View.OnClickListener btnListener) {
        final TextView title = (TextView)taskView.findViewById(R.id.title);
        title.setText(titleResId);
        
        setDescription(taskView, getString(descriptionResId));
        setChecked(taskView, checked);
        setButtonText(taskView, buttonTextResId);
        
        final Button btn = (Button)taskView.findViewById(R.id.add_btn);
        btn.setOnClickListener(btnListener);
    }
    
    protected void setChecked(View taskView, boolean checked) {
        final View checkIcon = taskView.findViewById(R.id.check_icon);
        checkIcon.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
    }
    
    protected boolean getChecked(View taskView) {
        final View checkIcon = taskView.findViewById(R.id.check_icon);
        return (checkIcon.getVisibility() == View.VISIBLE);
    }
    
    protected void setDescription(View taskView, String description) {
        final TextView descriptionView = (TextView)taskView.findViewById(R.id.description);
        descriptionView.setText(description);
    }
    
    protected void setButtonText(View taskView, int buttonTextResId) {
        final Button btn = (Button)taskView.findViewById(R.id.add_btn);
        btn.setText(buttonTextResId); 
    }
    
    private void capturePhoto() 
    {
        Date date = new Date();
        long time = date.getTime();
        String filename = getString(R.string.files_path) + time + ".jpg";
        
        File file = new File(Environment.getExternalStorageDirectory(), filename);
          
        mPhotoFilename = file.getPath();
        Log.i(TAG, "Photo filename = " + mPhotoFilename);
          
        Log.d(TAG, "Starting camera activity...");
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(intent, ACTIVITY_CAPTURE_PHOTO);
    }

    private void captureNote() 
    {
        Log.d(TAG, "Starting note activity...");
        Intent intent = new Intent(this, NoteEdit.class);
        String title = getString(R.string.note_title_prefix) + " " + mCommonName;
        intent.putExtra("title", title);
        startActivityForResult(intent, ACTIVITY_CAPTURE_NOTE);
    }
    
    private void captureQuantity() {
        Log.d(TAG, "Opening quantity dialog...");
        showDialog(DIALOG_QUANTITY);
    }
    
    private void updateQuantityTask() {
        String[] quantity_descriptions = getResources().getStringArray(R.array.tag_quantity_descriptions);
        setDescription(mQuantityTask, quantity_descriptions[mAmountId]);
    }
    
    private void updateLocationTask() {
        // TODO
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putLong("park_id", mParkId);
        outState.putString("common_name", mCommonName);
        outState.putString("science_name", mScienceName);
        outState.putString("note", mNote);
        outState.putInt("amount_id", mAmountId);
        outState.putBoolean("quantity_checked", getChecked(mQuantityTask));
        outState.putParcelable("location", mLocation);
        outState.putString("filename", mPhotoFilename);
        outState.putSerializable("type", mTagType);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_QUANTITY:
            dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.tag_tasks_title_quantity)
                        .setSingleChoiceItems(getResources().getStringArray(R.array.tag_quantity_choices), 
                                mAmountId, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mAmountId = which;
                                        mPreferences.edit().putInt("amount_id", mAmountId).commit();
                                        updateQuantityTask();
                                        TagTasks.this.setChecked(mQuantityTask, true);
                                        
                                        dialog.dismiss();
                                    }
                                })
                        .create();
            break;
        default:
            dialog = null;
            break;
        }
        return dialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACTIVITY_CAPTURE_PHOTO:
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Returned OK from Camera activity");
                
                File file = new File(mPhotoFilename);
                Bitmap image = null;
                
                try {
                    // Fix for HTC Sense Camera
                    // If "data" is not null, then we need to save that bitmap
                    // before we scale it
                    if(data != null)
                        image = (Bitmap)data.getExtras().get("data");
                    
                    if(image != null && !file.exists()) {
                        Log.d(TAG, "HTC Camera, creating file from data");
                        OutputStream os = new FileOutputStream(file);
                        image.compress(CompressFormat.JPEG, 100, os);
                        os.close();
                    }
                    
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    
                    options.inJustDecodeBounds= true;
                    BitmapFactory.decodeFile(mPhotoFilename, options);
                    
                    int factor = Math.max(options.outWidth / MAX_IMAGE_WIDTH, 
                            options.outHeight / MAX_IMAGE_HEIGHT);
                    
                    if(factor == 0)
                        factor = 1;
                    
                    Log.d(TAG, "Image scaling factor = " + factor);
                    
                    options.inSampleSize = msb32(factor);                    
                    options.inPurgeable = true;
                    options.inInputShareable = true;
                    options.inJustDecodeBounds= false;
                    image = BitmapFactory.decodeFile(mPhotoFilename, options);
                    Bitmap scaledImage;
                    
                    if(image == null) {
                        Log.e(TAG, "File '" + mPhotoFilename + "' cannot be decoded into a Bitmap!");
                    } else {  
                        if(image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
                            float scale = Math.min(MAX_IMAGE_WIDTH/(float)image.getWidth(), 
                                    MAX_IMAGE_HEIGHT/(float)image.getHeight());
                            int newWidth = (int) ((image.getWidth() * scale) + 0.5f);
                            int newHeight = (int) ((image.getHeight() * scale) + 0.5f);
                            
                            Log.i(TAG, "Scaling bitmap: width=" + newWidth + ", height=" + newHeight);
                            
                            scaledImage = Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
                            image.recycle();
                        } else {
                            // Since the original image was "purgeable"
                            // we have to reload the all bitmap data into memory
                            // or we'll erase the file when we write it back out
                            image.recycle();
                            scaledImage = BitmapFactory.decodeFile(mPhotoFilename);
                        }
                        
                        file = new File(mPhotoFilename);
                        OutputStream os = new FileOutputStream(file);
                        scaledImage.compress(CompressFormat.JPEG, 80, os);
                        os.close();
                        
                        // update the photo task view
                        setChecked(mPhotoTask, true);
                        setDescription(mPhotoTask, getString(R.string.tag_tasks_description_photo_replace));
                        setButtonText(mPhotoTask, R.string.tag_tasks_btn_change);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found!", e);
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception:", e);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory!", e);
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.out_of_memory_title)
                        .setMessage(R.string.out_of_memory_photo_msg)
                        .setPositiveButton(R.string.out_of_memory_photo_retake, 
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    new File(mPhotoFilename).delete();
                                    capturePhoto();
                                }
                            })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    
                    return;
                }
            }

            break;
        case ACTIVITY_CAPTURE_NOTE:
            if(resultCode == Activity.RESULT_OK) {
                mNote = data.getExtras().getString("note");
                
                // update the note task view
                setChecked(mNoteTask, true);
                setDescription(mNoteTask, mNote);
                setButtonText(mNoteTask, R.string.tag_tasks_btn_change);
            }
            break;
        }
    }
  
    private void recordObservation()
    {
        Log.d(TAG, "Recording observation: tag = " + mCommonName + ", amount id = " + mAmountId);
        
        if (saveToDatabase()) {
            Toast.makeText(getApplicationContext(), getString(R.string.invasive_mapped_notice), 5).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.invasive_mapping_failed), 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean saveToDatabase() {
        PhotoDatabase pdb = new PhotoDatabase(this);

        double longitude = 0.0;
        double latitude = 0.0;
        float accuracy = 0.0F;
        
        String tag = mCommonName;
        if (tag.equals("Other\nPlant")) {
            tag = "Other";
        }
        
        String amount = getResources().getStringArray(R.array.tag_quantity_choices)[mAmountId].toLowerCase();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        /* This is the new method of sending the date and time.
         * It uses the ISO 8601 standard which include time zone information.
         * TODO: Uncomment the following lines of code when the server-side code has been updated
        Calendar calendar = Calendar.getInstance();
        W3CDateFormat dateFormat = new W3CDateFormat(W3CDateFormat.Pattern.SECOND);
        */
        
        String time = dateFormat.format(calendar.getTime());

        // The logic here is this: if GPS is available and the lock happened
        // less than ten minutes ago, go with the GPS. Otherwise, try to use network
        // location.
        long gpsdelay = 60 * 10;

        LocationManager lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String text = "Using GPS\n";
        Location loc = lManager.getLastKnownLocation("gps");
        long curtime = System.currentTimeMillis() / 1000;
        
        if (loc == null || ((curtime - (loc.getTime() / 1000)) > gpsdelay)) {
            loc = lManager.getLastKnownLocation("network");
            text = "Using network\n";
        }

        if (loc != null) {
            text += (curtime - (loc.getTime() / 1000)) + " seconds since last lock.\n";
            
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
            accuracy = loc.getAccuracy();
        }
        
        Log.d("GPS INFO", text + "lat: " + latitude + "\nlng:" + longitude);

        if (tag.equals("notvalid") && (mPhotoFilename != null)) {
            File file = null;
            file = new File(mPhotoFilename.toString());

            if (file != null) {
                file.delete();
            }

            return false;
        } else {
            // Lets add it to a DB (the filename will be NULL if no picture was
            // taken)
            pdb.open();
            Log.d(TAG, "Adding observation to the database");
            long photo_created = pdb.createPhoto(longitude, latitude, time, accuracy,
                    mPhotoFilename, tag, mParkId, amount, mNote, mTagType.value());
            pdb.close();

            // start the upload service if auto upload is on because we now have
            // data to upload
            if (photo_created != -1
                    && mPreferences.getBoolean("upload_service_on", true)) {
                Log.d(TAG, "Starting Upload Service");
                Intent service = new Intent(this, UploadService.class);
                this.startService(service);

                mPreferences.edit().putBoolean("upload_service_on", true).commit();
            }

            if (photo_created == -1)
                return false;
            else
                return true;
        }
    }
    
    private int msb32(int x)
    {
        x |= (x >> 1);
        x |= (x >> 2);
        x |= (x >> 4);
        x |= (x >> 8);
        x |= (x >> 16);
        return (x & ~(x >> 1));
    }
    
//    public static boolean isLocationEnabled(final Activity activity) {
//        LocationManager manager = (LocationManager) activity
//                .getSystemService(Context.LOCATION_SERVICE);
//        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
//
//        if (!(manager.isProviderEnabled("gps") || manager.isProviderEnabled("network"))) {
//            AlertDialog alert = new AlertDialog.Builder(activity).setTitle(
//                    activity.getString(R.string.msg_gps_location_disabled))
//                    .setMessage(
//                            activity.getString(R.string.msg_gps_cannot_access))
//                    .setPositiveButton(android.R.string.ok,
//                            new OnClickListener() {
//
//                                public void onClick(DialogInterface dialog,
//                                        int which) {
//                                    Intent intent = new Intent(
//                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                                    activity.startActivityForResult(intent,
//                                                    WhatsInvasive.CHANGE_GPS_SETTINGS_2);
//
//                                }
//                            }).setNegativeButton(android.R.string.cancel,
//                            new OnClickListener() {
//
//                                public void onClick(DialogInterface dialog,
//                                        int which) {
//                                    activity
//                                            .showDialog(WhatsInvasive.BLOCKING_TAG);
//                                    preferences.edit()
//                                        .putBoolean("location_service_on", false)
//                                        .commit();
//                                    Intent intent = new Intent(activity, BlockingTagUpdate.class);
//                                    activity.startActivityForResult(intent,
//                                            WhatsInvasive.CHANGE_GPS_SETTINGS);
//
//                                }
//                            }).create();
//
//            alert.show();
//
//            return false;
//        }
//
//        return true;
//    }
}
