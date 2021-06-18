package com.example.mlmusicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.SongViewHolder> {

    private List<String> mSongs;
    private Context mContext;
    private SongClickListener listener;

    public static class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private final TextView songName;
        private SongClickListener listener;

        public SongViewHolder(@NonNull View itemVew, SongClickListener listener){
            super(itemVew);
            this.listener = listener;
            songName = itemVew.findViewById(R.id.songName);
            itemVew.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onItemClick(getAdapterPosition());
        }
    }

    public SongRecyclerAdapter(Context ct, List<String> songs, SongClickListener listener){
        mContext = ct;
        mSongs = songs;
        this.listener = listener;
    }
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.songs_recycler_item, parent, false);
        return new SongViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        holder.songName.setText(mSongs.get(position));
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }


}
