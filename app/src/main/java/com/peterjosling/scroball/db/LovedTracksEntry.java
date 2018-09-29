package com.peterjosling.scroball.db;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = ScroballDB.class)
public class LovedTracksEntry extends BaseModel {

  @PrimaryKey(autoincrement = true)
  public long id;

  @Column public String artist;

  @Column public String track;

  @Column public int status;
}
