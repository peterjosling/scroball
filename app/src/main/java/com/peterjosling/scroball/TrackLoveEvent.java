package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

/** Represents a request to love a track. */
@AutoValue
public abstract class TrackLoveEvent {

  public abstract Track track();

  public static TrackLoveEvent create(Track track) {
    return new AutoValue_TrackLoveEvent(track);
  }
}
