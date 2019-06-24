package com.peterjosling.scroball;

import android.media.session.PlaybackState;
import android.util.Log;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerState {

  private static final String TAG = PlayerState.class.getName();

  private final String player;
  private final Scrobbler scrobbler;
  private final ScrobbleNotificationManager notificationManager;
  private final EventBus eventBus = ScroballApplication.getEventBus();
  private PlaybackItem playbackItem;
  private Timer submissionTimer;

  public PlayerState(
      String player, Scrobbler scrobbler, ScrobbleNotificationManager notificationManager) {
    this.player = player;
    this.scrobbler = scrobbler;
    this.notificationManager = notificationManager;
    eventBus.register(this);
  }

  public void setPlaybackState(PlaybackState playbackState) {
    if (playbackItem == null) {
      return;
    }

    playbackItem.updateAmountPlayed();

    int state = playbackState.getState();
    boolean isPlaying = state == PlaybackState.STATE_PLAYING;

    if (isPlaying) {
      Log.d(TAG, "Track playing");
      postEvent(playbackItem.getTrack());
      playbackItem.startPlaying();
      notificationManager.updateNowPlaying(playbackItem.getTrack());
      scheduleSubmission();
    } else {
      Log.d(TAG, String.format("Track paused (state %d)", state));
      postEvent(Track.empty());
      playbackItem.stopPlaying();
      notificationManager.removeNowPlaying();
      scrobbler.submit(playbackItem);
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

    if (track.isSameTrack(currentTrack)) {
      Log.d(TAG, String.format("Track metadata updated: %s", track));

      // Update track in PlaybackItem, as this new one probably has updated details/more keys.
      playbackItem.setTrack(track);
    } else {
      Log.d(TAG, String.format("Changed track: %s", track));

      if (playbackItem != null) {
        playbackItem.stopPlaying();
        scrobbler.submit(playbackItem);
      }

      playbackItem = new PlaybackItem(track, now);
    }

    if (isPlaying) {
      postEvent(track);
      scrobbler.updateNowPlaying(track);
      notificationManager.updateNowPlaying(track);
      playbackItem.startPlaying();
      scheduleSubmission();
    }
  }

  private void scheduleSubmission() {
    Log.d(TAG, "Scheduling scrobble submission");

    if (submissionTimer != null) {
      submissionTimer.cancel();
    }

    long delay = scrobbler.getMillisecondsUntilScrobble(playbackItem);

    if (delay > -1) {
      Log.d(TAG, "Scrobble scheduled");
      submissionTimer = new Timer();
      submissionTimer.schedule(
          new TimerTask() {
            @Override
            public void run() {
              scrobbler.submit(playbackItem);
              scheduleSubmission();
            }
          },
          delay);
    }
  }

  private void postEvent(Track track) {
    eventBus.post(NowPlayingChangeEvent.builder().track(track).source(player).build());
  }

  @Subscribe
  private void onTrackLoved(TrackLovedEvent e) {
    if (playbackItem.isPlaying()) {
      // Track love state has changed - refresh the notification.
      notificationManager.updateNowPlaying(playbackItem.getTrack());
    }
  }
}
