package com.example.skybeatmusicplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {



    //animation variables;
    Animation topAnim,bottomAnim;
    ImageView imgBoombox;
    TextView tvMusic;

    private static  int SPLASH_SCREEN = 3000;



    //linked to StorageUtil
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.skybeatmusicplayer.PlayNewAudio";

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //taking the user permission
       // CheckUserPermsions();



       // loadAudio();

        //hooking the animation files
        topAnim = AnimationUtils.loadAnimation(this,R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottom_animation);

        imgBoombox = findViewById(R.id.imgBoombox);
        tvMusic = findViewById(R.id.tvMusic);

        imgBoombox.setAnimation(topAnim);
        tvMusic.setAnimation(bottomAnim);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this,HomeActivity.class);
                startActivity(intent);
                finish();
            }
        },SPLASH_SCREEN);



       // some how local audio file is not loading
        // have to work with that
        // audioList is not getting initiating
        //lets see what can we do

      // Toast.makeText(MainActivity.this, audioList.get(0).getTitle(), Toast.LENGTH_SHORT).show();

        //   playAudio(1);





        //testing online stream music works or not //works
       //playAudio("https://upload.wikimedia.org/wikipedia/commons/6/6c/Grieg_Lyric_Pieces_Kobold.ogg");


        //totally trying a new way to read file form storage
        //so .....
        //ArrayList<File> songs = readSongs(Environment.getExternalStorageDirectory());

        //Toast.makeText(MainActivity.this, songs.get(0).getName().toString(), Toast.LENGTH_SHORT).show();



    }

}