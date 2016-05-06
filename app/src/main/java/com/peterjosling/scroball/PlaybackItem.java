package com.peterjosling.scroball;

import org.immutables.value.Value;

@Value.Immutable
public abstract class PlaybackItem {

  public abstract Track track();
  public abstract long timestamp();
  public abstract long playbackStartTime();
  public abstract long amountPlayed();
  public abstract boolean isPlaying();

  public PlaybackItem updateAmountPlayed() {
    if (!isPlaying()) {
      return this;
    }

    long now = System.currentTimeMillis();
    long start = playbackStartTime();

    return ImmutablePlaybackItem.builder().from(this)
        .amountPlayed(amountPlayed() + now - start)
        .playbackStartTime(now)
        .build();
  }
}
