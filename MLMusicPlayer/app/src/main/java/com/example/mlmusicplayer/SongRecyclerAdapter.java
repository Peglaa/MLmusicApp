package com.example.mlmusicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.SongViewHolder> {

    private String[] mSongs;
    private Context mContext;

    public static class SongViewHolder extends RecyclerView.ViewHolder{

        private final TextView songName;

        public SongViewHolder(@NonNull View itemVew){
            super(itemVew);

            songName = itemVew.findViewById(R.id.songName);
        }
    }

    public SongRecyclerAdapter(Context ct, String[] songs){
        mContext = ct;
        mSongs = songs;
    }
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.songs_recycler_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        holder.songName.setText(mSongs[position]);
    }

    @Override
    public int getItemCount() {
        return mSongs.length;
    }


}
