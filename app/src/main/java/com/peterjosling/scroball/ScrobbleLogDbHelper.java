package com.peterjosling.scroball;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.VisibleForTesting;

import com.peterjosling.scroball.ScrobbleLogContract.ScrobbleLogEntry;

public class ScrobbleLogDbHelper extends SQLiteOpenHelper {

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
          " )";

  public static final int DATABASE_VERSION = 1;
  public static final String DATABASE_NAME = "ScrobbleLog.db";

  public ScrobbleLogDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  private ScrobbleLogDbHelper(Context context, String name) {
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
  public static ScrobbleLogDbHelper getTestInstance(Context context) {
    return new ScrobbleLogDbHelper(context, null);
  }
}
