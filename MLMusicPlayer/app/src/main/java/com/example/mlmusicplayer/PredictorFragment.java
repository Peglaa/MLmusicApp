package com.example.mlmusicplayer;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;

public class PredictorFragment extends Fragment {

    private TextView txtReady;
    private ImageView ivReady;
    private Button btnRecord, btnStop, btnPlay, btnModel, btnPredict;
    private MediaRecorder recorder = new MediaRecorder();
    private PyObject modelObject;
    private boolean isModelSetup = false;
    private String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";
    private ProgressBar progressModel, progressPrediction;
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
            }

        }
    };



    public PredictorFragment() {
        // Required empty public constructor
    }

    public static PredictorFragment newInstance() {
        return new PredictorFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_predictor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtReady = view.findViewById(R.id.txtReady);
        ivReady = view.findViewById(R.id.ivReady);
        ivReady.setVisibility(View.INVISIBLE);
        progressModel = view.findViewById(R.id.pbModel);
        progressModel.getIndeterminateDrawable().setColorFilter(Color.parseColor("#d16c00"), android.graphics.PorterDuff.Mode.SRC_IN);
        progressModel.setVisibility(View.INVISIBLE);
        progressPrediction = view.findViewById(R.id.pbPredict);
        progressPrediction.getIndeterminateDrawable().setColorFilter(Color.parseColor("#d16c00"), android.graphics.PorterDuff.Mode.SRC_IN);
        progressPrediction.setVisibility(View.INVISIBLE);
        btnPlay = view.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnPlayClicked();
            }
        });
        btnRecord = view.findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnRecordClicked();
            }
        });
        btnStop = view.findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnStopClicked();
            }
        });
        btnModel = view.findViewById(R.id.btnModel);
        btnModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnModelClicked();
            }
        });
        btnPredict = view.findViewById(R.id.btnPredict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnPredictClicked();
            }
        });

        if(isMicrophonePresent()) getMicrophonePermission();
        getStoragePermission();
    }

    public void onBtnRecordClicked(){

        try{
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(mFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();
        }catch (Exception e){
            e.printStackTrace();
        }

        Toast.makeText(getContext(), "Recording is started!", Toast.LENGTH_SHORT).show();

    }

    public void onBtnPlayClicked(){


    }

    public void onBtnStopClicked(){

        recorder.stop();
        recorder.release();
        Toast.makeText(getContext(), "Recording is stopped!", Toast.LENGTH_SHORT).show();
    }

    public void onBtnModelClicked(){

        setupModel();
    }

    public void onBtnPredictClicked(){

        if(!isModelSetup){
            Toast toast = Toast.makeText(requireContext(), "You need to setup the model first!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            btnModel.setEnabled(false);
            btnPredict.setEnabled(false);
            progressPrediction.setVisibility(View.VISIBLE);
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
                        PyObject pyObjectConvert = py.getModule("extractor");
                        PyObject pyobjConvert = pyObjectConvert.callAttr("convert_audio", mFilePath);
                        PyObject pyObjectPredict = py.getModule("classifier");
                        PyObject pyobj = pyObjectPredict.callAttr("full_prediction", "/storage/emulated/0/Music/recording.wav", modelObject);

                        Toast.makeText(getContext(), formatPrediction(pyobj.toString()),Toast.LENGTH_SHORT).show();

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

    private boolean isMicrophonePresent(){
        return requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private String getRecordingFilePath(){
        File musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "recording" + ".mp3");
        return file.getPath();
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
                        Log.i("TAG", "Creating ML model... ");

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

    private void getMicrophonePermission(){
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            int MICROPHONE_PERMISSION_CODE = 200;
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    private void getStoragePermission(){
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            int STORAGE_PERMISSION_CODE = 100;
            ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }
}