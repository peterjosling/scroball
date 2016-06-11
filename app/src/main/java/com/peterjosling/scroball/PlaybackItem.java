package com.peterjosling.scroball;

import java.util.Locale;

public class PlaybackItem {

  private final long timestamp;

  private Track track;
  private long amountPlayed;
  private long playbackStartTime;
  private boolean isPlaying;
  private int playsScrobbled;

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

  public void updateTrack(Track track) {
    this.track = track;
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

  public int getPlaysScrobbled() {
    return playsScrobbled;
  }

  public void addScrobble() {
    playsScrobbled++;
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
    playbackStartTime = now;
  }

  @Override
  public String toString() {
    return String.format(
        Locale.getDefault(),
        "PlaybackItem{Track=%s, timestamp=%d, isPlaying=%s, amountPlayed=%d, playbackStartTime=%d, " +
            "playsScrobbled=%d}",
        track.toString(),
        timestamp,
        Boolean.toString(isPlaying),
        amountPlayed,
        playbackStartTime,
        playsScrobbled);
  }
}
