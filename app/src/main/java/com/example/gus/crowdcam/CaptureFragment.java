package com.example.gus.crowdcam;


import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.loopj.android.http.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;


/**
 * A simple {@link Fragment} subclass.
 */
public class CaptureFragment extends Fragment {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    MediaMetadataRetriever metaRetriver;

    public CaptureFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        metaRetriver = new MediaMetadataRetriever();
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        Button record = (Button) view.findViewById(R.id.button1);
        record.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dispatchTakeVideoIntent();
            }
        });

        return view;
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == getActivity().RESULT_OK) {
            Uri videoUri = intent.getData();
            metaRetriver.setDataSource(getActivity(), videoUri);
            String selectedPath = getPath(videoUri);
            uploadVideo(selectedPath);
        }
    }

    private String getPath(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
    }

    private void uploadVideo(String videoPath) {
        String url = "http://crowdcam.cloudapp/net:5000";
        String datetime = "";
        String location;
        double lat = 0.0;
        double lng = 0.0;
        File file = null;
        try {
            file = new File(videoPath);

            datetime = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            location = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);

            final String OLD_FORMAT = "yyyyMMdd'T'HHmmss.SSS'Z'";
            final String NEW_FORMAT = "yyyy/MM/dd hh:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
            Date d = sdf.parse(datetime);
            sdf.applyPattern(NEW_FORMAT);
            datetime = sdf.format(d);
            lat = Double.valueOf(location.substring(0, location.length()/2));
            lng = Double.valueOf(location.substring(location.length()/2, location.length()));

            Log.d("Datetime: ", datetime);
            Log.d("Location: ", lat + " " + lng);
        } catch (Exception e) {
        }

        RequestParams params = new RequestParams();
        params.put("lat", Double.toString(lat));
        params.put("lng", Double.toString(lng));
        params.put("datetime", "data");
        try {
            params.put("file", file);
        } catch(FileNotFoundException e) {}

        UploadAPI.post("/uploads", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

}
