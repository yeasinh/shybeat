package com.example.skybeatmusicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
         MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener
{
    //Binder Given to clients
    private final IBinder iBinder = new LocalBinder();

    //MediaPlayer obj to use it for media playback
    public MediaPlayer mediaPlayer;
    //for storing the path of the audio file
    private String mediaFile;
    //for pause/resume MediaPlayer;
    private int resumePosition;
    private AudioManager audioManager;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    /**
     * Invoked when the audio focus of the system is updated
     * @param focusChange
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying())
                    pauseMedia();
                   // mediaPlayer.stop();
                //mediaPlayer.release();
               // mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }

    }

    /**
     * request the system for gaining the audio focus
     * @return
     */
    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    /**
     * remove the audio focus from the system
     * @return
     */
    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    /**
     * the system calls this method when an activity, requests the  service to be started
     * it initializes the MediaPlayer and makes the focus request
     * makes sure it does not throw a nullPointerException
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

       // if (mediaSessionManager == null) {
            //  initMediaSession();
            initMediaPlayer();
            //  buildNotification(PlaybackStatus.PLAYING);
       // }

        //Handle Intent action from MediaSession.TransportControls
       // handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Invoked  when the service will be destroyed
     * MediaPayer resources must be released
     * As the service is about to be destroyed
     * Broadcast Receivers should be unregistered
     * and the shared preference should be deleted
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
            //MediaPayer is stopped and released
        }
        removeAudioFocus();

        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

       // removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(pauseMusic);
        unregisterReceiver(resumeMusic);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

    /**
     * Invoked indicating buffering status of
     * a media resource being streamed over the network
     * @param mp
     * @param percent
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }
    


    /**
     * Invoked when playback of a media source has completed
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        stopMedia();
        //stop the service
        stopSelf();

        Intent broadcastIntent = new Intent(HomeActivity.Broadcast_PLAY_NEXT_MUSIC);
        sendBroadcast(broadcastIntent);

    }

    /**
     * Invoked when there has been an error during an asynchronous operation
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    /**
     * Invoked to communicate some info
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    /**
     * Invoked when the media source is ready for playback
     * @param mp
     */
    @Override
  public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        //playMedia();
        HomeActivity.seekBar.setMax(mediaPlayer.getDuration());
        playMedia();
       HomeActivity.changeSeekbar();

   }


    /**
     * Invoked indicating the completion of a seek operation.
     * @param mp
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {

        resumePosition = mediaPlayer.getCurrentPosition();
    }

    /**
     * Need to bind the service because it interact with the activity to get audio files
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Initializing the MediaPayer
     */
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    /**
     * for playing the audio
     */
    public void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            HomeActivity.imgPausePlay.setImageResource(R.drawable.ic_pause);
            showNotification("Pause");
        }
    }


    /**
     * for stop playing the Media
     */
    public void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            HomeActivity.imgPausePlay.setImageResource(R.drawable.ic_play);
            showNotification("Play");
        }
    }

    /**
     * for pausing the audio
     */
    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            HomeActivity.imgPausePlay.setImageResource(R.drawable.ic_play);
            showNotification("Play");
        }
    }

    /**
     * for resuming the audio
     */
    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();

            HomeActivity.imgPausePlay.setImageResource(R.drawable.ic_pause);
            HomeActivity.changeSeekbar();
            showNotification("Pause");
        }
    }


    /**
     * Broadcast Receiver is needed for when the audio become noisy ie. headphone removed
     * when the receiver receives the noise onReceive is invoked
     * and the music is paused
     */
    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            //buildNotification(PlaybackStatus.PAUSED);
        }
    };

    /**
     * The broadcast receiver needs to be registered
     * it registered the bocomeNoisyReceiver Broadcast receiver
     */
    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }



    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;


    /**
     * This function handles when the there is call going on
     * music would be paused during the call and resume after the call
     */
    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }


    //List of available Audio files
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently playing audio


    /**
     * this Broadcast receiver is for play new audio
     */
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
          //  updateMetaData();
          //  buildNotification(PlaybackStatus.PLAYING);
        }
    };

    /**
     * registering the play new audio broadcast receiver
     */
    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(HomeActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }


    //pause Broadcast Receiver
    private BroadcastReceiver pauseMusic = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            //pause the Music
            pauseMedia();
        }
    };
    //register the pause broadcast Receiver

    private void register_pauseMusic(){
        IntentFilter filter = new IntentFilter(HomeActivity.Broadcast_PAUSE_MUSIC);
        registerReceiver(pauseMusic, filter);
    }



    private BroadcastReceiver resumeMusic = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            //pause the Music
            resumeMedia();
        }
    };
    //register the resume broadcast Receiver

    private void register_resumeMusic(){
        IntentFilter filter = new IntentFilter(HomeActivity.Broadcast_RESUME_MUSIC);
        registerReceiver(resumeMusic, filter);
    }
    /**
     * Registering All the broadcast receivers
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();

        //pausing music
        register_pauseMusic();
        register_resumeMusic();
    }


   // notification part have to be done in this class

    public void showNotification(String playPause)
    {
        Intent playPauseIntent;
        if(playPause.equals("Play"))
        {
            playPauseIntent = new Intent(HomeActivity.Broadcast_RESUME_MUSIC);
        }
        else
        {
            playPauseIntent = new Intent(HomeActivity.Broadcast_PAUSE_MUSIC);
        }
        PendingIntent pauseIntent = PendingIntent.getBroadcast(this,0,playPauseIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        Intent nextMusicIntent = new Intent(HomeActivity.Broadcast_PLAY_NEXT_MUSIC);
        PendingIntent nextIntent = PendingIntent.getBroadcast(this,0,nextMusicIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextPreviousIntent = new Intent(HomeActivity.Broadcast_PLAY_PREVIOUS_MUSIC);
        PendingIntent prevIntent = PendingIntent.getBroadcast(this,0,nextPreviousIntent,PendingIntent.FLAG_UPDATE_CURRENT);



        Notification notification = new NotificationCompat.Builder(this, "Channel_2")
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_music)
                // Add media control buttons that invoke intents in your media service
                .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent) // #0
                .addAction(R.drawable.ic_pause, playPause, pauseIntent)  // #1
                .addAction(R.drawable.ic_skip_next, "Next",nextIntent)     // #2
                // Apply the media style template
                .setContentTitle(HomeActivity.audioList.get(audioIndex).getTitle())
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,notification);
    }





}
