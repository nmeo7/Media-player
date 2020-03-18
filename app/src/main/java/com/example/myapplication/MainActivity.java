package com.example.myapplication;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    private MediaBrowserCompat mediaBrowser;
    private static final String TAG = "ACTIVITY LOG";
    public static final String EXTRA_MESSAGE = "MSG";


    ArrayList<DataModel> dataModels;
    ListView listView;
    private static CustomAdapter adapter;

    MediaDbHelper mediaDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.i(TAG, "on create");

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MyService.class),
                connectionCallbacks,
                null);

        listView = findViewById(R.id.list);

        dataModels= new ArrayList<>();
        final MediaMetadata mediaMetadata = MediaMetadata.getInstance(this);

        adapter= new CustomAdapter(dataModels,getApplicationContext());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DataModel dataModel = mediaMetadata.getMedia( Integer.valueOf(dataModels.get(position).getId()) );

                Snackbar.make(view, dataModel.getTitle()+"\n"+dataModel.getUri()+" API: "+dataModel.getDuration(), Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();

                Bundle bundle = new Bundle();
                bundle.putString("id", dataModel.getId());

                // MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().sendCustomAction("changeMedia", bundle);

                Intent intent = new Intent(getApplicationContext(), SongActivity.class);
                String message = dataModel.getTitle()+"\n"+dataModel.getUri()+" API: "+dataModel.getDuration();
                intent.putExtra(EXTRA_MESSAGE, message);
                intent.putExtra("id", Integer.valueOf(dataModel.getId()));
                intent.putExtra(SongActivity.EXTRA_MEDIA_ID, String.valueOf(dataModel.getId()));
                startActivity(intent);

            }
        });

        dataModels = mediaMetadata.getMediaList();

        adapter= new CustomAdapter(dataModels,getApplicationContext());
        listView.setAdapter(adapter);


    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "on start");
        mediaBrowser.connect();
        Log.i(TAG, " == on start");
    }

    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();

    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {

                    Log.i(TAG, "media session connected");

                    try {
                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(MainActivity.this, // Context
                                        token);

                        MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

                        buildTransportControls();
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

    void buildTransportControls()
    {
        // Button playPause = findViewById(R.id.play_pause);

        Log.i(TAG, "build controls");

        /*
        playPause.setOnClickListener(new Button.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             int pbState = MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState();
                                             if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                                 Log.i(TAG, "was playing");
                                                 MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();
                                             } else {
                                                 Log.i(TAG, "was paused");
                                                 MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                                             }
                                         }
                                     }
        );*/

        final MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);

        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        mediaController.registerCallback(controllerCallback);
        mediaBrowser.subscribe( mediaBrowser.getRoot(), new MediaBrowserCompat.SubscriptionCallback () {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                super.onChildrenLoaded(parentId, children);
                Log.i(TAG, "on Children Loaded.");
            }
        });


    }

    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.i(TAG, "metadata changed");
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {

                    Log.i(TAG, "playback state changed " + MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState());
                }
            };


}
