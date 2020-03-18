package com.example.myapplication;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MediaMetadata {

    ArrayList<DataModel> dataModels;
    Context context;

    private static MediaMetadata mediaMetadata;
    private static String TAG = "MEDIAMETADATA_CLASS";

    DataModel currentMedia;
    int gettingPreparedMedia = 0;

    MediaPlayer player;

    private MediaMetadata (Context context)
    {
        this.context = context;
    }

    public static MediaMetadata getInstance(Context context)
    {
        if (mediaMetadata == null) mediaMetadata = new MediaMetadata(context);
        return mediaMetadata;
    }

    public ArrayList<DataModel> getMediaList ()
    {
        if (dataModels == null) dataModels = getAudios ();
        return dataModels;
    }

    public DataModel getCurrentMedia ()
    {
        return currentMedia;
    }

    public void setCurrentMedia (int index)
    {
        currentMedia = getMedia (index);
    }

    public DataModel getMedia(int index)
    {
        MediaDbHelper mediaDbHelper;
        mediaDbHelper = new MediaDbHelper(context);
        SQLiteDatabase db = mediaDbHelper.getReadableDatabase();

        Cursor cursor = db.query("media", null, "id=" + index, null, null, null, null);

        if(cursor.moveToNext()) {
            try {
                String title = cursor.getString( cursor.getColumnIndexOrThrow("title") );
                String duration = cursor.getString( cursor.getColumnIndexOrThrow("duration") );
                String uri = cursor.getString( cursor.getColumnIndexOrThrow("uri") );
                String genre = cursor.getString( cursor.getColumnIndexOrThrow("genre") );
                int lastPosition = cursor.getInt( cursor.getColumnIndexOrThrow("lastPosition") );

                Log.i(TAG, "getMedia: Media found from database.");
                Log.i(TAG, "getMedia: Last position: " + lastPosition);

                DataModel ret = new DataModel(String.valueOf(index), title, uri, genre, Integer.valueOf(duration));
                ret.setLastPosition(lastPosition);

                cursor.close();
                return ret;
            } catch (Exception e) {}
        }

        cursor.close();

        for (DataModel dataModel:dataModels) {
            if (dataModel.getId().equals( String.valueOf(index) ))
            {
                try {
                    db = mediaDbHelper.getWritableDatabase();

                    ContentValues values = new ContentValues();
                    values.put("id", dataModel.getId());
                    values.put("title", dataModel.getTitle());
                    values.put("uri", dataModel.getUri());
                    values.put("duration", dataModel.getDuration());
                    values.put("genre", dataModel.getGenre());
                    values.put("lastPosition", 0);

                    Log.i(TAG, "getMedia: " + db.insert("media", null, values));

                } catch (Exception e) {
                    Log.i(TAG, "getMedia: " + e.getMessage());
                }
                return dataModel;
            }
        }
        return null;
    }

    public void saveMediaLastPosition ()
    {
        if (currentMedia == null || player == null) return;
        MediaDbHelper mediaDbHelper = new MediaDbHelper(context);
        SQLiteDatabase db = mediaDbHelper.getWritableDatabase();

        int lastPosition = player.getCurrentPosition() / 1000 - 30;
        if (lastPosition < 0) lastPosition = 0;

        ContentValues values = new ContentValues();
        values.put("lastPosition", lastPosition);

        Log.i(TAG, "getMedia: save last played position " + db.update("media", values,"id=" + currentMedia.getId(), null ));
    }

    ArrayList<DataModel> getAudios ()
    {
        ArrayList<DataModel> AudioList = new ArrayList<DataModel>();

        try {

            String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";
            Cursor cursor = context.getContentResolver().query( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, "", null, sortOrder );

            int idColumn =      cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn =    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn =    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext()) {
                String id = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn).replace(".mp3", "");
                int duration = (cursor.getInt(durationColumn) + 500) / 1000;
                String uri = "content://media/external/audio/media/" + id;

                String genre = "music";

                if (duration > 60 * 7)
                    genre = "podcast or talkshow";

                if (duration > 60 * 60)
                    genre = "audio book";

                if (duration > 0)
                    AudioList.add( new DataModel(id, name, uri, genre, duration) );
            }
        }
        catch (Exception e)
        {
            Log.i(TAG, "getAudios: " + e.getMessage());
        }

        return AudioList;
    }

    boolean isPlaying (int index)
    {
        if (currentMedia == null || player == null) return false;
        return player.isPlaying() && (index == Integer.valueOf(currentMedia.getId()));
    }

    void prepare (int gettingPreparedMedia)
    {
        this.gettingPreparedMedia = gettingPreparedMedia;
    }

    MediaPlayer getPlayer ()
    {
        return this.player;
    }

    void setPlayer (MediaPlayer player)
    {
        this.player = player;
    }
}
