package com.example.flingphone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class UploadActivity extends AppCompatActivity {

    private final String TAG = UploadActivity.class.getSimpleName();

    private String storageBucketName;
    private String region;
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // not necessary because layout switched in main
        setContentView(R.layout.activity_upload);
        setStorageInfo();
        photoPath = getIntent().getStringExtra("path");
        Log.d(TAG, photoPath);

        uploadAndSave();
    }

    private void setStorageInfo() {
        JSONObject s3Config = new AWSConfiguration(this)
                .optJsonObject("S3TransferUtility");
        try {
            storageBucketName = s3Config.getString("Bucket");
            region = s3Config.getString("Region");
        } catch (JSONException e) {
            Log.e(TAG, "Can't find S3 bucket", e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UploadActivity.this,
                            "Error: Can't find S3 bucket. \nHave you run 'amplify add storage'? ",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    // Checks permissions for upload (required)
    private void uploadAndSave() {
        if (photoPath != null) {
            Log.e(TAG, "photoPath is not null" + photoPath);
            // For higher Android levels, we need to check permission at runtime
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted! Requesting...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
            // Upload a photo first. We will only call save on its successful callback.
            uploadWithTransferUtility(photoPath);
        }
    }
    private String getS3Key(String localPath) {
        //We have read and write ability under the public folder
        return "public/" + new File(localPath).getName();
    }
    public void uploadWithTransferUtility(String localPath) {
        String key = getS3Key(localPath);

        Log.d(TAG, "Uploading file from " + localPath + " to " + key);
        Log.d(TAG, "key is: " + key);
        File newfile = new File(localPath);
        if(ClientFactory.appSyncClient() == null){
            Log.e(TAG, "client is null please fix");
        }
        TransferObserver uploadObserver =
                ClientFactory.transferUtility().upload(
                        key,
                        newfile);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    Log.d(TAG, "Upload is completed. ");

                    // Don't think I need this
                    // Upload is successful. Save the rest and send the mutation to server.
                    //save();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Log.e(TAG, "Failed to upload photo. ", ex);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(UploadActivity.this, "Failed to upload photo", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });
    }


}
