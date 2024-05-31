package com.example.skybeatmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MusicActivity extends AppCompatActivity {

    // instance of the service
    private MediaPlayerService player;
    // status of the service
    boolean serviceBound = false;

    //array list of audio files;

    //ArrayList<Audio> audioList;

    //linked to StorageUtil
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.skybeatmusicplayer.PlayNewAudio";
    public static final String Broadcast_PAUSE_MUSIC = "com.example.skybeatmusicplayer.PauseMusic";

    TextView tvMusicTitle,tvMusicArtist;
    ImageView imgPausePlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        Intent intent = getIntent();
        int index = intent.getIntExtra("Audio_index",0);

        tvMusicTitle = findViewById(R.id.tvMusicTit);
        tvMusicArtist = findViewById(R.id.tvMusicArt);
        imgPausePlay = findViewById(R.id.imgPlayPaus);

        tvMusicTitle.setText(ApplicationClass.audioList.get(index).getTitle());



        if(ApplicationClass.audioList.get(index).getArtist().equals("<unknown>"))
        {
            tvMusicArtist.setText("Unknown Artist");
        }
        else
        {
            tvMusicArtist.setText(ApplicationClass.audioList.get(index).getArtist());
        }

        imgPausePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //when clicked
                sendBroadcast(new Intent(Broadcast_PAUSE_MUSIC));
                imgPausePlay.setImageResource(R.drawable.ic_pause);
            }
        });


        //playAudio(index);
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MusicActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(ApplicationClass.audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

}