package com.peterjosling.scroball;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.VisibleForTesting;

import com.peterjosling.scroball.ScroballDBContract.PendingPlaybackItemEntry;
import com.peterjosling.scroball.ScroballDBContract.ScrobbleLogEntry;

public class ScroballDBHelper extends SQLiteOpenHelper {

  private static final String TEXT_TYPE = " TEXT";
  private static final String INT_TYPE = " INTEGER";
  private static final String COMMA_SEP = ",";
  private static final String SQL_CREATE_ENTRIES =
      "CREATE TABLE " + ScrobbleLogEntry.TABLE_NAME + " (" +
          ScrobbleLogEntry._ID + " INTEGER PRIMARY KEY," +
          ScrobbleLogEntry.COLUMN_NAME_TIMESTAMP + INT_TYPE + COMMA_SEP +
          ScrobbleLogEntry.COLUMN_NAME_ARTIST + TEXT_TYPE + COMMA_SEP +
          ScrobbleLogEntry.COLUMN_NAME_ALBUM_ARTIST + TEXT_TYPE + COMMA_SEP +
          ScrobbleLogEntry.COLUMN_NAME_TRACK + TEXT_TYPE + COMMA_SEP +
          ScrobbleLogEntry.COLUMN_NAME_ALBUM + TEXT_TYPE + COMMA_SEP +
          ScrobbleLogEntry.COLUMN_NAME_STATUS + INT_TYPE +
          " );" +
          "CREATE TABLE " + PendingPlaybackItemEntry.TABLE_NAME + " (" +
          PendingPlaybackItemEntry._ID + " INTEGER PRIMARY KEY," +
          PendingPlaybackItemEntry.COLUMN_NAME_TIMESTAMP + INT_TYPE + COMMA_SEP +
          PendingPlaybackItemEntry.COLUMN_NAME_ARTIST + TEXT_TYPE + COMMA_SEP +
          PendingPlaybackItemEntry.COLUMN_NAME_ALBUM_ARTIST + TEXT_TYPE + COMMA_SEP +
          PendingPlaybackItemEntry.COLUMN_NAME_TRACK + TEXT_TYPE + COMMA_SEP +
          PendingPlaybackItemEntry.COLUMN_NAME_ALBUM + TEXT_TYPE + COMMA_SEP +
          PendingPlaybackItemEntry.COLUMN_NAME_AMOUNT_PLAYED + INT_TYPE +
          " );";

  public static final int DATABASE_VERSION = 1;
  public static final String DATABASE_NAME = "ScroballDB.db";

  public ScroballDBHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  private ScroballDBHelper(Context context, String name) {
    super(context, name, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_ENTRIES);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // TODO
  }

  @VisibleForTesting
  public static ScroballDBHelper getTestInstance(Context context) {
    return new ScroballDBHelper(context, null);
  }
}
