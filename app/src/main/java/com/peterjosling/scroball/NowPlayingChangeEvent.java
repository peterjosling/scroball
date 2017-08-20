package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NowPlayingChangeEvent {

  public abstract Track track();

  public abstract String source();

  public static Builder builder() {
    return new AutoValue_NowPlayingChangeEvent.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder track(Track track);

    public abstract Builder source(String source);

    public abstract NowPlayingChangeEvent build();
  }
}
