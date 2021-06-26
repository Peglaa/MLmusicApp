package com.example.mlmusicplayer;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AtomicFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.mlmusicplayer.ml.TfLiteModel;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.schema.TensorType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.metadata.schema.TensorMetadata;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongsFragment extends Fragment implements SongClickListener{

    private RecyclerView songRecycler;
    private TextView txtReady;
    private ImageView ivReady;
    private Button btnPredict, btnModel;
    private SongRecyclerAdapter songAdapter;
    private List<String> songs = new ArrayList<>();
    private ArrayList<File> mySongs = new ArrayList<>();
    private ArrayList<String> predictions = new ArrayList<>();
    private static final String TAG = "Predictor";
    private PyObject modelObject;
    private boolean isModelSetup = false;
    private ProgressBar progressModel, progressPrediction, progressPredictionHorizontal;
    private int progress;
    private Handler handler = new Handler(){

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            Bundle objBundle = msg.getData();

            if(objBundle.containsKey("MODEL")){
                btnModel.setEnabled(true);
                btnPredict.setEnabled(true);
                progressModel.setVisibility(View.INVISIBLE);
                txtReady.setTextColor(Color.parseColor("#1D6C00"));
                txtReady.setVisibility(View.VISIBLE);
                txtReady.setText("Model is ready!");
                ivReady.setVisibility(View.VISIBLE);
            }
            if(objBundle.containsKey("PREDICT")){
                btnModel.setEnabled(true);
                btnPredict.setEnabled(true);
                progressPrediction.setVisibility(View.INVISIBLE);
                progressPredictionHorizontal.setVisibility(View.INVISIBLE);
            }

        }
    };

    public static SongsFragment newInstance() {
        return new SongsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtReady = view.findViewById(R.id.txtReady);
        ivReady = view.findViewById(R.id.ivReady);
        ivReady.setVisibility(View.INVISIBLE);
        btnPredict = view.findViewById(R.id.btnPredict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                predictSongsGenre();
            }
        });
        btnModel = view.findViewById(R.id.btnModel);
        btnModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupModel();
            }
        });
        progressModel = view.findViewById(R.id.modelProgress);
        progressModel.getIndeterminateDrawable().setColorFilter(Color.parseColor("#d16c00"), android.graphics.PorterDuff.Mode.SRC_IN);
        progressModel.setVisibility(View.INVISIBLE);
        progressPrediction = view.findViewById(R.id.predictProgress);
        progressPrediction.getIndeterminateDrawable().setColorFilter(Color.parseColor("#d16c00"), android.graphics.PorterDuff.Mode.SRC_IN);
        progressPrediction.setVisibility(View.INVISIBLE);
        progressPredictionHorizontal = view.findViewById(R.id.predictProgressHorizontal);
        progressPredictionHorizontal.getProgressDrawable().setColorFilter(Color.parseColor("#d16c00"), android.graphics.PorterDuff.Mode.SRC_IN);
        progressPredictionHorizontal.setVisibility(View.INVISIBLE);
        songRecycler = view.findViewById(R.id.songs_recycler);
        songRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        runtimePermission();
    }

    public void runtimePermission() {
        Dexter.withContext(requireContext()).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        displaySongs();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

    }

    public ArrayList<File> findSongs(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        for (File singleFile : files) {
            if (singleFile.isDirectory() && !singleFile.isHidden()) {
                arrayList.addAll(findSongs(singleFile));
            } else {
                if (singleFile.getName().endsWith(".wav") || singleFile.getName().endsWith(".WAV") ) {
                    arrayList.add(singleFile);
                }
            }
        }

        return arrayList;
    }

    private void displaySongs() {
        mySongs = findSongs(Environment.getExternalStorageDirectory());
        Log.i(TAG, "SONGS: " + mySongs);

        for (int i = 0; i < mySongs.size(); i++) {
            songs.add(mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "").replace(".WAV", ""));
            Log.v("SONGS", songs.get(i));
        }

        songAdapter = new SongRecyclerAdapter(requireContext(), songs, predictions, this);
        songRecycler.setAdapter(songAdapter);


        songRecycler.addItemDecoration(new DividerItemDecoration(songRecycler.getContext(), DividerItemDecoration.VERTICAL));


    }

    @Override
    public void onItemClick(int position) {
        String songName = (String) songs.get(position);
        startActivity(new Intent(requireContext(), PlayerActivity.class)
                .putExtra("songs", mySongs)
                .putExtra("name", songName)
                .putExtra("pos", position));
    }

    private void setupModel() {
        if (isModelSetup) {
            Toast toast = Toast.makeText(requireContext(), "Model is already ready!", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            btnModel.setEnabled(false);
            btnPredict.setEnabled(false);
            progressModel.setVisibility(View.VISIBLE);
            Runnable objRunnable = new Runnable() {

                Message message = handler.obtainMessage();
                Bundle objBundle = new Bundle();

                @Override
                public void run() {
                    try {

                        if (! Python.isStarted()) {
                            Python.start(new AndroidPlatform(requireContext()));
                        }
                        Python py = Python.getInstance();
                        PyObject pyObject = py.getModule("classifier");
                        Log.i(TAG, "Creating ML model... ");

                        modelObject = pyObject.callAttr("setupModel", "/storage/emulated/0/Music/data.csv");
                        isModelSetup = true;

                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    objBundle.putString("MODEL", "model");
                    message.setData(objBundle);
                    handler.sendMessage(message);
                }
            };
            Thread objBackgroundThread = new Thread(objRunnable);
            objBackgroundThread.start();
        }
    }

    private void predictSongsGenre() {

        if(!isModelSetup){
            Toast toast = Toast.makeText(requireContext(), "You need to setup the model first!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            btnModel.setEnabled(false);
            btnPredict.setEnabled(false);
            progressPrediction.setVisibility(View.VISIBLE);
            progressPredictionHorizontal.setVisibility(View.VISIBLE);
            progressPredictionHorizontal.setProgress(0);
            progress = progressPredictionHorizontal.getProgress();
            Runnable objRunnable = new Runnable() {

                Message message = handler.obtainMessage();
                Bundle objBundle = new Bundle();

                @Override
                public void run() {
                    try {
                        if (! Python.isStarted()) {
                            Python.start(new AndroidPlatform(requireContext()));
                        }
                        Python py = Python.getInstance();
                        PyObject pyObject = py.getModule("classifier");
                        Log.i(TAG, "Extracting features... ");
                        int index = 0;
                        for (File song : mySongs) {
                            PyObject pyobj = pyObject.callAttr("full_prediction", song.toString(), modelObject);
                            Log.i(TAG, "PREDICTION: " + pyobj);
                            predictions.add(formatPrediction(pyobj.toString()));
                            // Get a handler that can be used to post to the main thread
                            progress += (100 + mySongs.size() -1)/mySongs.size();
                            int finalIndex = index;
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnableGenre = new Runnable() {
                                @Override
                                public void run() {
                                    songAdapter.notifyItemChanged(finalIndex);
                                    progressPredictionHorizontal.setProgress(progress);
                                } // This is your code
                            };
                            mainHandler.post(myRunnableGenre);
                            index++;
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    objBundle.putString("PREDICT", "predict");
                    message.setData(objBundle);
                    handler.sendMessage(message);
                }
            };
            Thread objBackgroundThread = new Thread(objRunnable);
            objBackgroundThread.start();
        }


    }

    private String formatPrediction(String prediction){
        String pred1 = prediction.replace("[", "");
        String pred2 = pred1.replace("]", "");
        int num = Integer.parseInt(pred2);
        switch(num){
            default:
                return "Genre";
            case 0:
                return "Blues";
            case 1:
                return "Classical";
            case 2:
                return "Country";
            case 3:
                return "Disco";
            case 4:
                return "HipHop";
            case 5:
                return "Jazz";
            case 6:
                return "Metal";
            case 7:
                return "Pop";
            case 8:
                return "Reggae";
            case 9:
                return "Rock";
        }
    }


}