package com.peterjosling.scroball;

import android.media.session.PlaybackState;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerState {

  private final Scrobbler scrobbler;
  private final ScrobbleNotificationManager notificationManager;
  private PlaybackItem playbackItem;
  private Timer submissionTimer;

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
      scheduleSubmission();
    } else {
      playbackItem.stopPlaying();
      notificationManager.removeNowPlaying();
      scrobbler.submit(playbackItem);
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

    if (track.equals(currentTrack)) {
      // Update track in PlaybackItem, as this new one probably has updated details/more keys.
      playbackItem.setTrack(track);
    } else {
      System.out.println("Creating new PlaybackItem");

      if (playbackItem != null) {
        playbackItem.stopPlaying();
        scrobbler.submit(playbackItem);
      }

      playbackItem = new PlaybackItem(track, now);
    }

    if (isPlaying) {
      scrobbler.updateNowPlaying(track);
      notificationManager.updateNowPlaying(track);
      playbackItem.startPlaying();
      scheduleSubmission();
    }
  }

  private void scheduleSubmission() {
    if (submissionTimer != null) {
      submissionTimer.cancel();
    }

    long delay = scrobbler.getMillisecondsUntilScrobble(playbackItem);

    if (delay > -1) {
      submissionTimer = new Timer();
      submissionTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          scrobbler.submit(playbackItem);
          scheduleSubmission();
        }
      }, delay);
    }
  }
}
