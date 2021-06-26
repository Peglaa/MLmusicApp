package com.example.mlmusicplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    Button btnPlay, btnNext, btnPrevious, btnFastForward, btnFastRewind;
    TextView txtSongName, txtStart, txtStop;
    SeekBar seekMusic;

    String songName;
    static MediaPlayer mediaPlayer;
    int position;
    ArrayList<File> mySongs;
    private Thread updateseekbar;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        btnPlay = findViewById(R.id.btnPlay);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnFastForward = findViewById(R.id.btnFastForward);
        btnFastRewind = findViewById(R.id.btnFastRewind);
        txtSongName = findViewById(R.id.txtSong);
        txtStart = findViewById(R.id.txtStart);
        txtStop = findViewById(R.id.txtStop);
        seekMusic = findViewById(R.id.seekBar);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        mySongs = (ArrayList) bundle.getParcelableArrayList("songs");
        songName = i.getStringExtra("name");
        position = bundle.getInt("pos", 0);
        txtSongName.setSelected(true);
        Uri uri = Uri.parse(mySongs.get(position).toString());
        songName = mySongs.get(position).getName();
        txtSongName.setText(songName);

        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.start();

        updateseekbar = new Thread() {
            @Override
            public void run() {
                int totalDuration = mediaPlayer.getDuration();

                int currentPosition = 0;

                while (currentPosition < totalDuration) {
                    try {
                        sleep(500);
                        currentPosition = mediaPlayer.getCurrentPosition();
                        seekMusic.setProgress(currentPosition);
                    } catch (InterruptedException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        seekMusic.setMax(mediaPlayer.getDuration());
        updateseekbar.start();
        seekMusic.getProgressDrawable().setColorFilter(getResources().getColor(R.color.orange), PorterDuff.Mode.MULTIPLY);
        seekMusic.getThumb().setColorFilter(getResources().getColor(R.color.orange), PorterDuff.Mode.SRC_IN);

        seekMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        String endTime = createTime(mediaPlayer.getDuration());
        txtStop.setText(endTime);

        final Handler handler = new Handler();
        final int delay = 1000;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime = createTime(mediaPlayer.getCurrentPosition());
                txtStart.setText(currentTime);
                handler.postDelayed(this, delay);
            }
        }, delay);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    btnPlay.setBackgroundResource(R.drawable.ic_play);
                    mediaPlayer.pause();
                } else {
                    btnPlay.setBackgroundResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                }
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btnNext.performClick();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                position = (position + 1) % mySongs.size();
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                songName = mySongs.get(position).getName();
                txtSongName.setText(songName);
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                String endTime = createTime(mediaPlayer.getDuration());
                txtStop.setText(endTime);
                seekMusic.setMax(mediaPlayer.getDuration());
                seekMusic.setProgress(0);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        btnNext.performClick();
                    }
                });
                /*PlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mediaPlayer != null){
                            String currentTime = createTime(mediaPlayer.getCurrentPosition());
                            txtStart.setText(currentTime);
                        }
                        handler.postDelayed(this, 1000);
                    }
                });*/
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                position = ((position - 1) < 0) ? (mySongs.size() - 1) : (position - 1);
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                songName = mySongs.get(position).getName();
                txtSongName.setText(songName);
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                String endTime = createTime(mediaPlayer.getDuration());
                txtStop.setText(endTime);
                seekMusic.setMax(mediaPlayer.getDuration());
                seekMusic.setProgress(0);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        btnNext.performClick();
                    }
                });

                /*PlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mediaPlayer != null){
                            String currentTime = createTime(mediaPlayer.getCurrentPosition());
                            txtStart.setText(currentTime);
                        }
                        handler.postDelayed(this, 1000);
                    }
                });*/
            }
        });
    }

    public String createTime(int duration) {
        String time = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        time += min + ":";

        if (sec < 10) {
            time += "0";
        }
        time += sec;

        return time;
    }

}
