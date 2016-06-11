package com.peterjosling.scroball;

import android.media.session.PlaybackState;

public class PlayerState {

  private final Scrobbler scrobbler;
  private final ScrobbleNotificationManager notificationManager;
  private PlaybackItem playbackItem;

  public PlayerState(Scrobbler scrobbler, ScrobbleNotificationManager notificationManager) {
    this.scrobbler = scrobbler;
    this.notificationManager = notificationManager;
  }

  public void setPlaybackState(PlaybackState playbackState) {
    if (playbackItem == null) {
      return;
    }

    playbackItem.updateAmountPlayed();

    int state = playbackState.getState();
    boolean isPlaying = state == PlaybackState.STATE_PLAYING;

    if (isPlaying) {
      playbackItem.startPlaying();
      notificationManager.updateNowPlaying(playbackItem.getTrack());
    } else {
      playbackItem.stopPlaying();
      notificationManager.removeNowPlaying();
    }

    System.out.print("State: ");

    switch (state) {
      case PlaybackState.STATE_BUFFERING:
        System.out.println("Buffering");
        break;

      case PlaybackState.STATE_CONNECTING:
        System.out.println("Connecting");
        break;

      case PlaybackState.STATE_ERROR:
        System.out.println("Error");
        break;

      case PlaybackState.STATE_FAST_FORWARDING:
        System.out.println("Fast forwarding");
        break;

      case PlaybackState.STATE_NONE:
        System.out.println("None");
        break;

      case PlaybackState.STATE_PAUSED:
        System.out.println("Paused");
        break;

      case PlaybackState.STATE_PLAYING:
        System.out.println("Playing");
        break;

      case PlaybackState.STATE_REWINDING:
        System.out.println("Rewinding");
        break;

      case PlaybackState.STATE_SKIPPING_TO_NEXT:
        System.out.println("Skipping to next");
        break;

      case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
        System.out.println("Skipping to previous");
        break;

      case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
        System.out.println("Skipping to queue item");
        break;

      case PlaybackState.STATE_STOPPED:
        System.out.println("Stopped");
        break;
    }
  }

  public void setTrack(Track track) {
    Track currentTrack = null;
    boolean isPlaying = false;
    long now = System.currentTimeMillis();

    if (playbackItem != null) {
      currentTrack = playbackItem.getTrack();
      isPlaying = playbackItem.isPlaying();
    }

    if (!track.equals(currentTrack)) {
      System.out.println("Creating new PlaybackItem");

      if (playbackItem != null) {
        playbackItem.stopPlaying();
        scrobbler.submit(playbackItem);
      }

      scrobbler.updateNowPlaying(track);
      notificationManager.updateNowPlaying(track);

      playbackItem = new PlaybackItem(track, now);

      if (isPlaying) {
        playbackItem.startPlaying();
      }
    }
  }
}
