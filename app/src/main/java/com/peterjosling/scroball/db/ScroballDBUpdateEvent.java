package com.peterjosling.scroball.db;

import com.google.auto.value.AutoValue;
import com.peterjosling.scroball.Scrobble;

@AutoValue
public abstract class ScroballDBUpdateEvent {

  public abstract Scrobble scrobble();

  static ScroballDBUpdateEvent create(Scrobble scrobble) {
    return new AutoValue_ScroballDBUpdateEvent(scrobble);
  }
}
