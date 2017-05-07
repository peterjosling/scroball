package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ScroballDBUpdateEvent {

  public abstract Scrobble scrobble();

  public static ScroballDBUpdateEvent create(Scrobble scrobble) {
    return new AutoValue_ScroballDBUpdateEvent(scrobble);
  }
}
