package com.example.skybeatmusicplayer;

import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder>
{
    private ArrayList<Audio> songs;
    ItemClicked activity;

    public interface ItemClicked
    {
        void onItemClicked(int index);
    }

    public SongAdapter(Context context,ArrayList<Audio> list)
    {
        songs = list;
        //connecting context
        activity = (ItemClicked) context;
    }


    public class ViewHolder extends RecyclerView.ViewHolder
    {

        TextView tvTitle,tvAlbum,tvArtist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAlbum = itemView.findViewById(R.id.tvAlbum);
            tvArtist = itemView.findViewById(R.id.tvArtist);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // when clicked
                    activity.onItemClicked(songs.indexOf((Audio)v.getTag()));
                }
            });
        }
    }

    @NonNull
    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongAdapter.ViewHolder holder, int position) {

        holder.itemView.setTag(songs.get(position));

        if(songs.get(position).getTitle().isEmpty())
        {
            holder.tvTitle.setText("Unknown Title");
        }
        else
        {
            holder.tvTitle.setText(songs.get(position).getTitle());
        }

        if(songs.get(position).getArtist().equals("<unknown>"))
        {
            holder.tvArtist.setText("Unknown Artist");
        }
        else
        {
            holder.tvArtist.setText(songs.get(position).getArtist());
        }

        if(songs.get(position).getAlbum().isEmpty())
        {
            holder.tvAlbum.setText("Unknown Album");
        }
        else
        {
            holder.tvAlbum.setText(songs.get(position).getAlbum());
        }




    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
