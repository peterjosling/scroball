package com.peterjosling.scroball;

public class PlaybackItem {

  private final Track track;
  private final long timestamp;

  private long amountPlayed;
  private long playbackStartTime;
  private boolean isPlaying;

  public PlaybackItem(Track track, long timestamp) {
    this.track = track;
    this.timestamp = timestamp;
  }

  public PlaybackItem(Track track, long timestamp, long amountPlayed) {
    this(track, timestamp);
    this.amountPlayed = amountPlayed;
  }

  public Track getTrack() {
    return track;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getAmountPlayed() {
    return amountPlayed;
  }

  public boolean isPlaying() {
    return isPlaying;
  }

  public void startPlaying() {
    if (!isPlaying) {
      playbackStartTime = System.currentTimeMillis();
    }

    isPlaying = true;
  }

  public void stopPlaying() {
    updateAmountPlayed();
    isPlaying = false;
  }

  public void updateAmountPlayed() {
    if (!isPlaying()) {
      return;
    }

    long now = System.currentTimeMillis();
    long start = playbackStartTime;
    amountPlayed += now - start;
  }
}
