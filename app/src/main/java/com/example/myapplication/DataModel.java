package com.example.myapplication;


public class DataModel {

    private String title;
    private String uri;
    private String genre;
    private int duration;
    private int lastPosition;
    private String id;

    public DataModel(String id, String title, String uri, String genre, int duration) {
        this.id = id;
        this.title=title;
        this.uri=uri;
        this.genre=genre;
        this.duration=duration;
    }

    public String getTitle() {
        return title;
    }

    public String getUri() {
        return uri;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public String getDurationString() {

        String durationString = "";
        if (duration/3600 > 0)
            durationString = duration/3600 + ":";
        if (!durationString.equals("") && (duration/60)%60 < 10)
            durationString += "0";

        durationString += (duration/60)%60 + ":";

        if (duration%60 < 10)
            durationString += "0";

        durationString += duration%60;

        return durationString;
    }

    public int getLastPosition ()
    {
        return lastPosition;
    }

    public String getId()
    {
        return id;
    }

    public void setLastPosition (int lastPosition)
    {
        this.lastPosition = lastPosition;
    }
}
