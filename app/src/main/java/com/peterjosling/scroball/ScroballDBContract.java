package com.peterjosling.scroball;

import android.provider.BaseColumns;

public final class ScroballDBContract {

  public ScroballDBContract() {}

  public static abstract class ScrobbleLogEntry implements BaseColumns {
    public static final String TABLE_NAME = "scrobbles";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_ARTIST = "artist";
    public static final String COLUMN_NAME_ALBUM_ARTIST = "album_artist";
    public static final String COLUMN_NAME_TRACK = "track";
    public static final String COLUMN_NAME_ALBUM = "album";
    public static final String COLUMN_NAME_STATUS = "status";
  }

  public static abstract class PendingPlaybackItemEntry implements BaseColumns {
    public static final String TABLE_NAME = "pending";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_ARTIST = "artist";
    public static final String COLUMN_NAME_ALBUM_ARTIST = "album_artist";
    public static final String COLUMN_NAME_TRACK = "track";
    public static final String COLUMN_NAME_ALBUM = "album";
    public static final String COLUMN_NAME_AMOUNT_PLAYED = "amount_played";
  }
}
