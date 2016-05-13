package com.peterjosling.scroball;

import org.immutables.value.Value;

@Value.Immutable
public abstract class PlaybackItem {

  public abstract Track track();
  public abstract long timestamp();

  @Value.Default
  public long amountPlayed() {
    return 0;
  }

  @Value.Default
  public long playbackStartTime() {
    return 0;
  }

  @Value.Default
  public boolean isPlaying() {
    return false;
  }


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
