package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

/** Event triggered after a track has been successfully loved. */
@AutoValue
public abstract class TrackLovedEvent {

  public static TrackLovedEvent create() {
    return new AutoValue_TrackLovedEvent();
  }
}
