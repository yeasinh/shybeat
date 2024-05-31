package com.example.skybeatmusicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements SongAdapter.ItemClicked, View.OnClickListener {

    // instance of the service
    private static MediaPlayerService player;
    // status of the service
    boolean serviceBound = false;

    //array list of audio files;

    public static ArrayList<Audio> audioList;

    //linked to StorageUtil
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.skybeatmusicplayer.PlayNewAudio";
    public static final String Broadcast_PAUSE_MUSIC = "com.example.skybeatmusicplayer.PauseMusic";
    public static final String Broadcast_PLAY_NEXT_MUSIC = "com.example.skybeatmusicplayer.PlayNextMusic";
    public static final String Broadcast_RESUME_MUSIC = "com.example.skybeatmusicplayer.ResumeMusic";
    public static final String Broadcast_PLAY_PREVIOUS_MUSIC = "com.example.skybeatmusicplayer.PlayPreviousMusic";


    RecyclerView recyclerView;
    RecyclerView.Adapter myAdapter;
    RecyclerView.LayoutManager layoutManager;

    //adding Fragments
    Fragment listFrag, MusicFrag;
    FragmentManager fragmentManager;

    //
    TextView tvMusicTitle, tvMusicArtist;
    public static ImageView imgPausePlay, imgSkipNext, imgSkipPrevious;
    public static SeekBar seekBar;

    public static Runnable runnable;
    public static Handler handler;

    // for the back button
    private int clickCount = 0;

    //
    public static int currentSong = -1;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fragmentManager = getSupportFragmentManager();

        listFrag = fragmentManager.findFragmentById(R.id.ListFrag);
        MusicFrag = fragmentManager.findFragmentById(R.id.MusicFrag);

        tvMusicTitle = findViewById(R.id.tvMusicTit);
        tvMusicArtist = findViewById(R.id.tvMusicArt);
        imgPausePlay = findViewById(R.id.imgPlayPaus);
        imgSkipNext = findViewById(R.id.imgSkipNxt);
        imgSkipPrevious = findViewById(R.id.imgSkipPrevus);
        imgPausePlay.setImageResource(R.drawable.ic_pause);
        seekBar = findViewById(R.id.seekBar);


        fragmentManager.beginTransaction()
                .show(listFrag)
                .hide(MusicFrag)
                .commit();

        recyclerView = findViewById(R.id.listSongs);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //taking the user permission
        CheckUserPermsions();
        //loading the audio from the device
       // loadAudio();

        //setting up the adapter
       // myAdapter = new SongAdapter(this, audioList);
       // recyclerView.setAdapter(myAdapter);

       // myAdapter.notifyDataSetChanged();

        imgPausePlay.setOnClickListener(this);
        imgSkipNext.setOnClickListener(this);
        imgSkipPrevious.setOnClickListener(this);

        handler = new Handler();


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        register_playNextMusic();
        register_playPreviousMusic();

        // playAudio(1);

    }


    public static void changeSeekbar() {

        seekBar.setProgress(player.mediaPlayer.getCurrentPosition());


        if (player.mediaPlayer.isPlaying()) {
            runnable = HomeActivity::changeSeekbar;
            handler.postDelayed(runnable, 50);
        }

    }


    //Binding this Client to the AudioPlayer Service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(HomeActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
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
            storage.storeAudio(audioList);
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

        unregisterReceiver(playNextMusic);
        unregisterReceiver(playPreviousMusic);
    }

    /**
     * using content Resolver to get the audio files of local device
     * this function will load the audio data in to audioList instance
     */

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = getApplicationContext().getContentResolver().query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        assert cursor != null;
        cursor.close();
    }

    /**
     * File can not be read unless you have permission from the user
     * this is form taking the permission from the user
     */

    @RequiresApi(api = Build.VERSION_CODES.R)
    void CheckUserPermsions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
            else
            {
                loadAudio();

                //setting up the adapter
                myAdapter = new SongAdapter(this, audioList);
                recyclerView.setAdapter(myAdapter);

                myAdapter.notifyDataSetChanged();
            }
        }


    }

    //get acces to location permsion
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudio();

                //setting up the adapter
                myAdapter = new SongAdapter(this, audioList);
                recyclerView.setAdapter(myAdapter);

                myAdapter.notifyDataSetChanged();

            } else {
                // Permission Denied
                Toast.makeText(this, "Permission Denial", Toast.LENGTH_SHORT)
                        .show();
                CheckUserPermsions();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && clickCount == 1) {
            fragmentManager.beginTransaction()
                    .show(listFrag)
                    .hide(MusicFrag)
                    .commit();
            clickCount--;
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onItemClicked(int index) {

        tvMusicTitle.setText(audioList.get(index).getTitle());


        if (audioList.get(index).getArtist().equals("<unknown>")) {
            tvMusicArtist.setText("Unknown Artist");
        } else {
            tvMusicArtist.setText(audioList.get(index).getArtist());
        }

        fragmentManager.beginTransaction()
                .hide(listFrag)
                .show(MusicFrag)
                .commit();

        clickCount++;
        // Intent intent = new Intent(HomeActivity.this,MusicActivity.class);
        // intent.putExtra("Audio_index",index);
        //   startActivity(intent);

        if (currentSong != index) {
            playAudio(index);
            currentSong = index;
        }


    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgPlayPaus:
                if (player.mediaPlayer.isPlaying()) {
                    player.pauseMedia();
                  //  imgPausePlay.setImageResource(R.drawable.ic_play);
                } else {
                    player.resumeMedia();
                   // imgPausePlay.setImageResource(R.drawable.ic_pause);
                }
                break;
            case R.id.imgSkipNxt:
                currentSong++;
                if (currentSong == audioList.size()) {
                    playAudio(0);
                    currentSong = 0;
                } else {
                    playAudio(currentSong);
                }
                tvMusicTitle.setText(audioList.get(currentSong).getTitle());


                if (audioList.get(currentSong).getArtist().equals("<unknown>")) {
                    tvMusicArtist.setText("Unknown Artist");
                } else {
                    tvMusicArtist.setText(audioList.get(currentSong).getArtist());
                }
                break;
            case R.id.imgSkipPrevus:
                currentSong--;
                if (currentSong == -1) {
                    playAudio(audioList.size() - 1);
                    currentSong = audioList.size() - 1;
                } else {
                    playAudio(currentSong);
                }
                tvMusicTitle.setText(audioList.get(currentSong).getTitle());


                if (audioList.get(currentSong).getArtist().equals("<unknown>")) {
                    tvMusicArtist.setText("Unknown Artist");
                } else {
                    tvMusicArtist.setText(audioList.get(currentSong).getArtist());
                }

                break;


        }
    }

    private final BroadcastReceiver playNextMusic = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            //play the next Music
            currentSong++;
            if (currentSong == audioList.size()) {
                playAudio(0);
                currentSong = 0;
            } else {
                playAudio(currentSong);
            }
            tvMusicTitle.setText(audioList.get(currentSong).getTitle());


            if (audioList.get(currentSong).getArtist().equals("<unknown>")) {
                tvMusicArtist.setText("Unknown Artist");
            } else {
                tvMusicArtist.setText(audioList.get(currentSong).getArtist());
            }
        }
    };
    //register the next Music broadcast Receiver

    private void register_playNextMusic(){
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_NEXT_MUSIC);
        registerReceiver(playNextMusic, filter);
    }


    private final BroadcastReceiver playPreviousMusic = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            //play the previous Music
            currentSong--;
            if (currentSong == -1) {
                playAudio(audioList.size() - 1);
                currentSong = audioList.size() - 1;
            } else {
                playAudio(currentSong);
            }
            tvMusicTitle.setText(audioList.get(currentSong).getTitle());


            if (audioList.get(currentSong).getArtist().equals("<unknown>")) {
                tvMusicArtist.setText("Unknown Artist");
            } else {
                tvMusicArtist.setText(audioList.get(currentSong).getArtist());
            }
        }
    };
    //register the previous Music broadcast Receiver

    private void register_playPreviousMusic(){
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_PREVIOUS_MUSIC);
        registerReceiver(playPreviousMusic, filter);
    }
}


