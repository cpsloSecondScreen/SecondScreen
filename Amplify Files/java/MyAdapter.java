package com.example.amplifyapplication;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.amplify.generated.graphql.ListPetsQuery;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<ListPetsQuery.Item> mData = new ArrayList<>();;
    private LayoutInflater mInflater;
    // fix TAG error
    private final String TAG = MyAdapter.class.getSimpleName();


    // data is passed into the constructor
    MyAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListPetsQuery.Item item = mData.get(position);
        holder.txt_name.setText(item.name());
        holder.txt_description.setText(item.description());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // resets the list with a new set of data
    public void setItems(List<ListPetsQuery.Item> items) {
        mData = items;
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_name;
        TextView txt_description;
        ImageView image_view;
        String localUrl;

        ViewHolder(View itemView) {
            super(itemView);
            txt_name = itemView.findViewById(R.id.txt_name);
            txt_description = itemView.findViewById(R.id.txt_description);
            image_view = itemView.findViewById(R.id.image_view);
        }
        void bindData(ListPetsQuery.Item item) {
            txt_name.setText(item.name());
            txt_description.setText(item.description());

            if (item.photo() != null) {
                if (localUrl == null) {
                    downloadWithTransferUtility(item.photo());
                } else {
                    image_view.setImageBitmap(BitmapFactory.decodeFile(localUrl));
                }
            }
            else
                image_view.setImageBitmap(null);
        }

        private void downloadWithTransferUtility(final String photo) {
            final String localPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + photo;

            TransferObserver downloadObserver =
                    ClientFactory.transferUtility().download(
                            photo,
                            new File(localPath));

            // Attach a listener to the observer to get state update and progress notifications
            downloadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        // Handle a completed upload.
                        localUrl = localPath;
                        image_view.setImageBitmap(BitmapFactory.decodeFile(localPath));
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;

                    Log.d(TAG, "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
                }

                @Override
                public void onError(int id, Exception ex) {
                    // Handle errors
                    Log.e(TAG, "Unable to download the file.", ex);
                }
            });
        }
    }
}

