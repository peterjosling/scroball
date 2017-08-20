package com.peterjosling.scroball.db;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = ScroballDB.class)
public class ScrobbleLogEntry extends BaseModel {

  @PrimaryKey(autoincrement = true)
  public long id;

  @Column public int timestamp;

  @Column public String artist;

  @Column public String albumArtist;

  @Column public String album;

  @Column public String track;

  @Column public int status;
}
