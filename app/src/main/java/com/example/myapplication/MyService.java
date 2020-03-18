package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.List;

public class MyService extends MediaBrowserServiceCompat {
    private static final String LOG_TAG = "MEDIA SESSION LOG";
    private static final String TAG = "SERVICE LOG";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static int FOREGROUND_ID=1338;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private Context context;

    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
    // private MediaStyleNotification myPlayerNotification;
    private MediaBrowserService service;
    private MediaPlayer player;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private AudioFocusRequest audioFocusRequest;

    private String currentMediaUri;


    class Audio {
        private final Uri uri;
        private final String name;
        private final int duration;
        private final int size;

        public Audio(Uri uri, String name, int duration, int size) {
            this.uri = uri;
            this.name = name;
            this.duration = duration;
            this.size = size;

            Log.i(TAG, "Audio: " + uri + ", " + name);
        }
    }

    private void displayNotification ()
    {
        // Given a media session and its context (usually the component containing the session)
// Create a NotificationCompat.Builder

// Get the session's metadata

        Log.i(TAG, "Display Notification");

        MediaControllerCompat controller = mediaSession.getController();
        // MediaMetadataCompat mediaMetadata = controller.getMetadata();
        // MediaDescriptionCompat description = mediaMetadata.getDescription();

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, "117");

        builder
                // Add the metadata for the currently playing track
                .setContentTitle("title")
                .setContentText("subtitle")
                .setSubText("description")
                // .setLargeIcon( R.raw.brabus )

                // Enable launching the player by clicking the notification
                .setContentIntent(controller.getSessionActivity())

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)

                // Add an app icon and set its accent color
                // Be careful about the color
                .setSmallIcon(R.raw.brabus)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))

                // Add a pause button
                .addAction(new androidx.core.app.NotificationCompat.Action(
                        R.raw.brabus, "pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                // Take advantage of MediaStyle features
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)

                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP)));


// Display the notification and place the service in the foreground
        this.startForeground(1337, builder.build());


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(1337, builder.build());



        Log.i(TAG,  " // Display Notification");

    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "On create");

        context = getApplicationContext();

        mediaSession = new MediaSessionCompat(context, LOG_TAG);

        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {

                MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());

                if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
                    player.start();
                if (focusChange < 0)
                    player.pause();

                // mediaMetadata.playing = player.isPlaying();

                Log.i(TAG, "focus changed => " + focusChange);
            }
        };

        mediaSession.setCallback(callback);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                                 Bundle rootHints) {
        Log.i(TAG, "get root");
        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId,
                               final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.i(TAG, "get children");
        result.sendResult( new ArrayList<MediaBrowserCompat.MediaItem>() );
    }

    MediaSessionCompat.Callback callback = new
            MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    Log.i(TAG, "on play");

                    /*
                    if (player != null)
                    {
                        if (player.isPlaying())
                        {
                            onPause();
                            return;
                        }
                    }*/

                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    // Request audio focus for playback, this registers the afChangeListener
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setOnAudioFocusChangeListener(afChangeListener)
                            .setAudioAttributes(attrs)
                            .build();
                    int result = am.requestAudioFocus(audioFocusRequest);

                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                        startService(new Intent(context, MyService.class));

                        mediaSession.setActive(true);

                        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
                        // Put the service in the foreground, post notification
                        // service.startForeground(id, myPlayerNotification);
                        displayNotification ();
                        MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());

                        mediaMetadata.saveMediaLastPosition ();

                        try {
                            int mediaId = mediaMetadata.gettingPreparedMedia;

                            if (mediaMetadata.getCurrentMedia() != mediaMetadata.getMedia(mediaId) && player != null)
                            {
                                player.stop();
                                player.release();
                                player = null;
                            }

                            mediaMetadata.setCurrentMedia(mediaId);
                            String mediaUri = mediaMetadata.getCurrentMedia().getUri();

                            Log.i(TAG, "onPlay: " + mediaUri);
                            // Log.i(TAG, "onPlay: " + mediaId);

                            Uri myUri = Uri.parse(mediaUri); // initialize Uri here

                            if (player == null)
                            {
                                player = new MediaPlayer();
                                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                player.setDataSource(getApplicationContext(), myUri );
                                player.prepare();

                                int startFromPosition = mediaMetadata.getCurrentMedia().getLastPosition() * 1000;
                                player.seekTo(startFromPosition);
                            }

                            mediaMetadata.setPlayer(player);

                            player.start();

                            // mediaMetadata.setCurrentPlaying(mediaId);
                        }
                        catch (Exception e)
                        {

                        }

                        mediaMetadata.prepare(-1);

                    }
                }

                @Override
                public void onStop() {
                    Log.i(TAG, "on stop");
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    // Abandon audio focus
                    am.abandonAudioFocusRequest(audioFocusRequest);
                    unregisterReceiver(myNoisyAudioStreamReceiver);
                    // Stop the service
                    service.stopSelf();
                    // Set the session inactive  (and update metadata and state)
                    mediaSession.setActive(false);
                    // stop the player (custom call)
                    player.stop();
                    MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());
                    // mediaMetadata.playing = false;
                    // Take the service out of the foreground
                    service.stopForeground(false);
                }

                @Override
                public void onPause() {
                    Log.i(TAG, "on pause");
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    // Update metadata and state
                    // pause the player (custom call)
                    player.pause();
                    // unregister BECOME_NOISY BroadcastReceiver
                    unregisterReceiver(myNoisyAudioStreamReceiver);
                    // Take the service out of the foreground, retain the notification
                    // stopForeground(false);

                    MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());
                }

                @Override
                public void onCustomAction(String action, Bundle extras) {

                    if (action.equals("replay"))
                    {
                        player.seekTo(player.getCurrentPosition() - 30000);
                    }
                    else if (action.equals("forward"))
                    {
                        player.seekTo(player.getCurrentPosition() + 10000);
                    }
                    else{
                        String songId = extras.getString("id" );
                        Log.i(TAG, "on some custom action " + extras.getString("id" ) );
                        super.onCustomAction(action, extras);

                        currentMediaUri = "content://media/external/audio/media/" + songId;
                    }
                }
            };

    @Override
    public void onDestroy() {
        super.onDestroy();

        MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());
        mediaMetadata.saveMediaLastPosition ();
    }

    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                Log.i(TAG, "becoming noisy");
                player.pause();
                MediaMetadata mediaMetadata = MediaMetadata.getInstance(getApplicationContext());
                // mediaMetadata.playing = false;
            }
        }
    }


}
