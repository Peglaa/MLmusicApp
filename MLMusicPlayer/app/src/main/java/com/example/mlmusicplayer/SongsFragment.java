package com.example.mlmusicplayer;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class SongsFragment extends Fragment {

    private RecyclerView songRecycler;
    private SongRecyclerAdapter songAdapter;
    private String[] songs;

    public SongsFragment() {
        // Required empty public constructor
    }

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
                if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav")) {
                    arrayList.add(singleFile);
                }
            }
        }

        return arrayList;
    }

    private void displaySongs() {
        final ArrayList<File> mySongs = findSongs(Environment.getExternalStorageDirectory());

        songs = new String[mySongs.size()];

        for (int i = 0; i < mySongs.size(); i++) {
            songs[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");
            Log.v("SONGS", songs[i]);
        }

        songAdapter = new SongRecyclerAdapter(requireContext(), songs);
        songRecycler.setAdapter(songAdapter);
        songRecycler.addItemDecoration(new DividerItemDecoration(songRecycler.getContext(), DividerItemDecoration.VERTICAL));
    }
}