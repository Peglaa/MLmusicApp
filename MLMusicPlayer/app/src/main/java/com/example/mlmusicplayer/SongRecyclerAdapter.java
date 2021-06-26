package com.example.mlmusicplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.SongViewHolder> {

    private List<String> mSongs, predictions;
    private Context mContext;
    private SongClickListener listener;

    public static class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private final TextView songName;
        private TextView txtPrediction;
        private SongClickListener listener;
        private RelativeLayout itemLayout;

        public SongViewHolder(@NonNull View itemVew, SongClickListener listener){
            super(itemVew);
            this.listener = listener;
            songName = itemVew.findViewById(R.id.songName);
            txtPrediction = itemVew.findViewById(R.id.txtPrediction);
            itemLayout = itemVew.findViewById(R.id.itemLayout);
            itemVew.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onItemClick(getAdapterPosition());
        }
    }

    public SongRecyclerAdapter(Context ct, List<String> songs, List<String> predictions, SongClickListener listener){
        mContext = ct;
        mSongs = songs;
        this.listener = listener;
        this.predictions = predictions;
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
        if(predictions.size() != 0){
            holder.txtPrediction.setText(predictions.get(position));
            holder.txtPrediction.setTextColor(Color.parseColor("#1D6C00"));

        }
        else{
            holder.txtPrediction.setText("Genre?");
        }
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }


}
