package com.example.amplifyapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.CreatePetMutation;
import com.amazonaws.amplify.generated.graphql.ListPetsQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import javax.annotation.Nonnull;

import type.CreatePetInput;

public class AddPetActivity extends AppCompatActivity {

    // get rid of TAG error
    private final String TAG = AddPetActivity.class.getSimpleName();


    private static int RESULT_LOAD_IMAGE = 1;
    private String photoPath;
    private String storageBucketName;
    private String region;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        Button btnAddItem = findViewById(R.id.btn_save);
        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadAndSave();
            }
        });

        Button btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnAddPhoto.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                choosePhoto();
            }
        });

        setStorageInfo();
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
                    Toast.makeText(AddPetActivity.this,
                            "Error: Can't find S3 bucket. \nHave you run 'amplify add storage'? ",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void save() {
        CreatePetInput input = getCreatePetInput();

        CreatePetMutation addPetMutation = CreatePetMutation.builder()
                .input(input)
                .build();

        ClientFactory.appSyncClient().mutate(addPetMutation).
                refetchQueries(ListPetsQuery.builder().build()).
                enqueue(mutateCallback);

        //old
        //ClientFactory.appSyncClient().mutate(addPetMutation).enqueue(mutateCallback);
    }

    private void uploadAndSave(){
        if (photoPath != null) {
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
        } else {
            save();
        }
    }

    public void choosePhoto() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            // String picturePath contains the path of selected Image
            photoPath = picturePath;
        }
    }

    private String getS3Key(String localPath) {
        //We have read and write ability under the public folder
        return "public/" + new File(localPath).getName();
    }

    public void uploadWithTransferUtility(String localPath) {
        String key = getS3Key(localPath);

        Log.d(TAG, "Uploading file from " + localPath + " to " + key);

        TransferObserver uploadObserver =
                ClientFactory.transferUtility().upload(
                        key,
                        new File(localPath));

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    Log.d(TAG, "Upload is completed. ");

                    // Upload is successful. Save the rest and send the mutation to server.
                    save();
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
                        Toast.makeText(AddPetActivity.this, "Failed to upload photo", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });
    }
    // create different types of CreatePetInput based on whether photo selected or not
    private CreatePetInput getCreatePetInput() {
        final String name = ((EditText) findViewById(R.id.editTxt_name)).getText().toString();
        final String description = ((EditText) findViewById(R.id.editText_description)).getText().toString();

        if (photoPath != null && !photoPath.isEmpty()){
            /*
            final S3ObjectInput s3ObjectInput = S3ObjectInput.builder()
                    .bucket(storageBucketName)
                    .key(getS3Key(photoPath))
                    .region(region)
                    .localUri(photoPath)
                    .mimeType("image/jpg").build();

                    Requires changing .photo in return statement as well
             */
            return CreatePetInput.builder()
                    .name(name)
                    .description(description)
                    .photo(getS3Key(photoPath)).build();
        } else {
            return CreatePetInput.builder()
                    .name(name)
                    .description(description)
                    .build();
        }
    }

    // Mutation callback code
    private GraphQLCall.Callback<CreatePetMutation.Data> mutateCallback = new GraphQLCall.Callback<CreatePetMutation.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<CreatePetMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AddPetActivity.this, "Added pet", Toast.LENGTH_SHORT).show();
                    AddPetActivity.this.finish();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull final ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("", "Failed to perform AddPetMutation", e);
                    Toast.makeText(AddPetActivity.this, "Failed to add pet", Toast.LENGTH_SHORT).show();
                    AddPetActivity.this.finish();
                }
            });
        }
    };
}
