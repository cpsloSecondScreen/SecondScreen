package com.example.flingphone;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.controller.DiscoveryController;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private ListView deviceView;
    private Button refresh;
    private TextView target;
    private String ip;
    private String photoPath;

    private ProgressBar mProgressBar;
    private Button btnScreenshot;
    DiscoveryController mController;
    RemoteMediaPlayer mCurrentDevice;

    List<RemoteMediaPlayer> mDeviceList = new ArrayList<>();
    String[] deviceNames;


    private DiscoveryController.IDiscoveryListener mDiscovery = new DiscoveryController.IDiscoveryListener() {
        @Override
        public void playerDiscovered(RemoteMediaPlayer player) {
            //add media player to the application’s player list.
            if(!mDeviceList.contains(player)){
                mDeviceList.add(player);
            }
        }
        @Override
        public void playerLost(RemoteMediaPlayer player) {
            //remove media player from the application’s player list.
            if(mDeviceList.contains(player)){
                mDeviceList.remove(player);
            }
        }
        @Override
        public void discoveryFailure() {
           // Toast.makeText(getApplicationContext(), "Discovery Failure", Toast.LENGTH_LONG).show();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ClientFactory.init(this);
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        setupUI();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mController.start("com.your.organization.TVPlayer", mDiscovery);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mController.stop();
    }

    private void setupUI(){
        deviceView = findViewById(R.id.deviceList);
        mController = new DiscoveryController(getApplicationContext());
        refresh = findViewById(R.id.refresh);
        target = findViewById(R.id.target);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceNames = new String[mDeviceList.size()];
                int i = 0;
                for(RemoteMediaPlayer remoteMediaPlayer : mDeviceList){
                    deviceNames[i] = remoteMediaPlayer.getName();
                    i++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);
                deviceView.setAdapter(adapter);
            }
        });

        deviceView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentDevice = mDeviceList.get(position);
                try{
                    //oast.makeText(getApplicationContext(), "ismuted(): " + mCurrentDevice.getName(), Toast.LENGTH_LONG).show();
                    //TODO put this TextView on next screen for short pop-up?
                    target.setText("connected to " + mCurrentDevice.getUniqueIdentifier());
                    setContentView(R.layout.activity_screenshot);
                    initScreenshotButton();

                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }

    private void initScreenshotButton() {
        btnScreenshot = findViewById(R.id.btn_take_screenshot);
        btnScreenshot.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                takeScreenshot();
            }
        });
    }

    private void takeScreenshot() {
        /* TODO would like user to take another screenshot if they dislike current one
         * currently this is not possible
         */
        mCurrentDevice.sendCommand(ip).getAsync(new ErrorResultHandler("send command", "send command failure"));
        // new thread to run communication to FireTV for file transfer
        Thread t = new Thread(new Server());
        t.start();
        // wait for file transfer to complete before continuing
        // TODO this might crash because it's in main UI thread not sure
        // tested separately did not cause issues...
        btnScreenshot.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        try {
            t.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "done waiting for transfer thread");
        //TODO swap to image view, wait for user to accept or decline image

        //TODO create accept button logic to place this in
        Intent startUpload = new Intent(MainActivity.this, UploadActivity.class);
        startUpload.putExtra("path", photoPath);
        startActivity(startUpload);
    }

    private class ErrorResultHandler implements RemoteMediaPlayer.FutureListener<Void> {
        private String mCommand;
        private String mMsg;

        ErrorResultHandler(String command, String msg) {
            mCommand = command;
            mMsg = msg;
        }

        @Override
        public void futureIsNow(Future<Void> result) {
            try {
                result.get();
                Log.i(TAG, mCommand + ": successful");
            } catch(ExecutionException e) {
                Log.i(TAG, mMsg);
                e.printStackTrace();
            } catch(Exception e) {
                Log.i(TAG, mMsg);
                e.printStackTrace();
            }
        }
    }

    class Server implements Runnable{

        @Override
        public void run() {
            try{
                ServerSocket serverSocket = new ServerSocket(8888);
                Socket client = serverSocket.accept();

                byte[] buffer = new byte[1024];
                int read = 0;
                InputStream instream = client.getInputStream();

                Date now = new Date();
                android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                // use class variable photoPath so can access elsewhere in file
                photoPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + now + ".jpg";
                FileOutputStream fos = new FileOutputStream(photoPath);

                read = instream.read(buffer);

                while(read > 0){
                    fos.write(buffer, 0, read);
                    read = instream.read(buffer);
                }

                instream.close();
                fos.close();
                Log.d(TAG, "file transfer completed...");

            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
