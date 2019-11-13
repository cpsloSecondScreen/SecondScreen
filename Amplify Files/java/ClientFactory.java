package com.example.amplifyapplication;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.sigv4.CognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

public class ClientFactory {
    private static volatile AWSAppSyncClient client;
    private static volatile TransferUtility transferUtility;

    public static synchronized void init(final Context context) {
        if (client == null) {
            final AWSConfiguration awsConfiguration = new AWSConfiguration(context);
            client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfiguration)
                    .cognitoUserPoolsAuthProvider(new CognitoUserPoolsAuthProvider() {
                        @Override
                        public String getLatestAuthToken() {
                            try {
                                return AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                            } catch (Exception e) {
                                Log.e("APPSYNC_ERROR", e.getLocalizedMessage());
                                return e.getLocalizedMessage();
                            }
                        }
                    }).build();
        }
        if (transferUtility == null) {
            transferUtility = TransferUtility.builder()
                    .context(context)
                    .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                    .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                    .build();
        }
    }

    public static synchronized AWSAppSyncClient appSyncClient() {
        return client;
    }
    public static synchronized TransferUtility transferUtility() {
        return transferUtility;
    }
}