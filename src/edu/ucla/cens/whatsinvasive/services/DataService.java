package edu.ucla.cens.whatsinvasive.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import edu.ucla.cens.whatsinvasive.data.WhatsInvasiveProviderContract;

public class DataService extends IntentService {
    private static final String TAG = "DataService";
    
    public static final int RUNNING = 1;
    public static final int SUCCESS = 2;
    public static final int FAILED = 3;
    public static final int NOT_MODIFIED = 4;
    
    public static final int HTTP_GET = 0;
    public static final int HTTP_POST = 1;
    
    private static final int CONN_TIMEOUT = 60000;
    private static final int SO_TIMEOUT = 60000;

    public DataService() {
        super("edu.ucla.cens.whatsinvasive.services.DataService");
    }
    
    private String getBundleString(Bundle bundle, String key, String def) {
        String result = bundle.getString(key);
        if(result == null) {
            return def;
        }
        
        return result;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String scheme = getBundleString(intent.getExtras(), "scheme", "http");
        String host = getBundleString(intent.getExtras(), "host", WhatsInvasiveProviderContract.API_HOST);
        int port = intent.getIntExtra("port", 80);
        String path = intent.getStringExtra("path");
        Bundle data = intent.getBundleExtra("data");
        int method = intent.getIntExtra("method", HTTP_GET);
        ResultReceiver receiver = intent.getParcelableExtra("receiver");
        
        if(TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path must be provided");
        }
        
        if(data == null) {
            throw new IllegalArgumentException("data must be provided");
        }
        
        if(receiver == null) {
            throw new IllegalArgumentException("receiver must be provided");
        }
        
        receiver.send(RUNNING, null);
        
        DefaultHttpClient client = new DefaultHttpClient();
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONN_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);
        
        BufferedReader reader = null;
        final HttpUriRequest request;
        int resultCode;
        Bundle result = new Bundle();
        
        try { 
            switch(method) {
            case HTTP_GET:
                request = getGetRequest(scheme, host, port, path, data);
                break;
            case HTTP_POST:
                request = getPostRequest(scheme, host, port, path, data);
                break;
            default:
                throw new IllegalArgumentException("Unknown HTTP method: " + method);
            }
        
            HttpResponse response = client.execute(request);
            
            final int status = response.getStatusLine().getStatusCode();
            result.putInt("status", status);

            if (status == HttpStatus.SC_OK) {
                reader = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();

                String line = null;
                String newLine = System.getProperty("line.seperator");
                while ((line = reader.readLine()) != null) {
                    sb.append(line + newLine);
                }
                
                result.putString("data", sb.toString());
                resultCode = SUCCESS;
            } else if(status == HttpStatus.SC_NOT_MODIFIED){
                resultCode = NOT_MODIFIED;
            } else {
                resultCode = FAILED;
            }
        } catch (Exception e) {
            resultCode = FAILED;
            result.putString("error", e.getMessage());
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    result.putString("error", e.getMessage());
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
        
        receiver.send(resultCode, result);
    }
    
    private HttpUriRequest getGetRequest(String scheme, String host, int port, String path, Bundle params) throws URISyntaxException {
        ArrayList<NameValuePair> qparams = new ArrayList<NameValuePair>();
        
        for(String key : params.keySet()) {
            Object val = params.get(key);

            qparams.add(new BasicNameValuePair(key, val.toString()));
        }
        
        URI uri = URIUtils.createURI(scheme, host, port, path, URLEncodedUtils.format(qparams, "UTF-8"), null);
        
        return new HttpGet(uri);
    }
    
    private HttpUriRequest getPostRequest(String scheme, String host, int port, String path, Bundle params)
            throws URISyntaxException, UnsupportedEncodingException {
        URI uri = URIUtils.createURI(scheme, host, port, path, null, null);
        MultipartEntity entity = new MultipartEntity();
        
        for(String key : params.keySet()) {
            Object val = params.get(key);
            
            if(val instanceof File) {
                File file = (File)val;
                String extension = getExtension(file);
                MimeTypeMap map = MimeTypeMap.getSingleton();
                String mimeType = map.getMimeTypeFromExtension(extension);
                
                if(mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                
                entity.addPart(key, new FileBody(file, mimeType));
            } else {
                entity.addPart(key, new StringBody(val.toString()));
            }
        }
        
        HttpPost request = new HttpPost(uri);
        request.setEntity(entity);
        
        return request;
    }
    
    private String getExtension(File file) {
        String filename = file.getName();
        if(filename != null) {
            int index = filename.lastIndexOf('.');
            if(index != -1) {
                return filename.substring(index + 1);
            }
        }
        
        return null;
    }
}
