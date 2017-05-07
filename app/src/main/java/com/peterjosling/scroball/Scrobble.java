package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Scrobble {

  public abstract Track track();
  public abstract int timestamp();
  public abstract ScrobbleStatus status();
  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_Scrobble.Builder().status(new ScrobbleStatus(0));
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder track(Track track);
    abstract Builder timestamp(int timestamp);
    abstract Builder status(ScrobbleStatus status);
    abstract Scrobble build();
  }
}
