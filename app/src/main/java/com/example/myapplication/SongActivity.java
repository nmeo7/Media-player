package com.example.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.InputStream;

public class SongActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SONG LOG";
    public static final String EXTRA_MEDIA_ID = "SongActivity.MediaId";
    private MediaBrowserCompat mediaBrowser;
    private int mediaId;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        mediaId = intent.getIntExtra("id", -1);

        final MediaMetadata mediaMetadata = MediaMetadata.getInstance(this);

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.lyrics);
        textView.setText(message);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                MediaControllerCompat.getMediaController(SongActivity.this).getTransportControls().pause();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        InputStream imageStream = getResources().openRawResource( R.raw.cover );
        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

        ImageView imageView = findViewById( R.id.media_background );
        imageView.setImageBitmap( bitmap );

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MyService.class),
                connectionCallbacks,
                null);

        ImageView replay = findViewById(R.id.replay);
        final ImageView play_pause = findViewById(R.id.play_pause);
        ImageView forward = findViewById(R.id.forward);

        final SeekBar seekBar = findViewById(R.id.media_seek);
        seekBar.setMax( mediaMetadata.getMedia(mediaId).getDuration() );

        replay.setOnClickListener(this);
        play_pause.setOnClickListener(this);
        forward.setOnClickListener(this);

        SongActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaMetadata.isPlaying(mediaId))
                {
                    seekBar.setProgress( mediaMetadata.getPlayer().getCurrentPosition() / 1000 );
                }
                if (mediaMetadata.isPlaying(mediaId)) play_pause.setImageResource(R.drawable.ic_pause_black_24dp);
                else play_pause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                mHandler.postDelayed(this, 500);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaMetadata.isPlaying(mediaId))
                {
                    mediaMetadata.getPlayer().seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaBrowser.disconnect();
    }

    @Override
    public void onClick(View v) {
        MediaMetadata mediaMetadata = MediaMetadata.getInstance(this);

        if (v.getId() == R.id.play_pause && mediaMetadata.isPlaying(mediaId)) MediaControllerCompat.getMediaController(SongActivity.this).getTransportControls().pause();
        if (v.getId() == R.id.play_pause && !mediaMetadata.isPlaying(mediaId))
        {
            mediaMetadata.prepare(mediaId);
            MediaControllerCompat.getMediaController(SongActivity.this).getTransportControls().play();
        }
        if (v.getId() == R.id.replay) MediaControllerCompat.getMediaController(SongActivity.this).getTransportControls().sendCustomAction("replay", new Bundle());
        if (v.getId() == R.id.forward) MediaControllerCompat.getMediaController(SongActivity.this).getTransportControls().sendCustomAction("forward", new Bundle());

        ImageView play_pause = findViewById(R.id.play_pause);
        if (mediaMetadata.isPlaying(mediaId)) play_pause.setImageResource(R.drawable.ic_pause_black_24dp);
        else play_pause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {

                    Log.i(TAG, "media session connected");

                    try {
                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                        MediaControllerCompat mediaController = new MediaControllerCompat(SongActivity.this, token);
                        MediaControllerCompat.setMediaController(SongActivity.this, mediaController);
                    }
                    catch (Exception e)
                    {
                        Log.i(TAG, e.getLocalizedMessage());
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    Log.i(TAG, "media session connection suspended");
                }

                @Override
                public void onConnectionFailed() {
                    Log.i(TAG, "media session connection failed");
                }
            };
}
