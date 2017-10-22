package com.peterjosling.scroball;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.common.base.Optional;
import com.peterjosling.scroball.db.ScroballDB;

import java.util.ArrayList;
import java.util.List;

import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;

public class Scrobbler {

  private static final String TAG = Scrobbler.class.getName();
  private static final int SCROBBLE_THRESHOLD = 4 * 60 * 1000;
  private static final int MINIMUM_SCROBBLE_TIME = 30 * 1000;
  private static final int MAX_SCROBBLES = 50;

  private final LastfmClient client;
  private final ScrobbleNotificationManager notificationManager;
  private final ScroballDB scroballDB;
  private final ConnectivityManager connectivityManager;
  private final List<PlaybackItem> pendingPlaybackItems;
  private final List<Scrobble> pending;
  private boolean isScrobbling = false;
  private long lastScrobbleTime = 0;
  private long nextScrobbleDelay = 0;

  public Scrobbler(
      LastfmClient client,
      ScrobbleNotificationManager notificationManager,
      ScroballDB scroballDB,
      ConnectivityManager connectivityManager) {
    this.client = client;
    this.notificationManager = notificationManager;
    this.scroballDB = scroballDB;
    this.connectivityManager = connectivityManager;
    // TODO write unit test to ensure non-network plays get scrobbled with duration lookup.
    this.pendingPlaybackItems = new ArrayList<>(scroballDB.readPendingPlaybackItems());
    this.pending = new ArrayList<>(scroballDB.readPendingScrobbles());
  }

  public void updateNowPlaying(Track track) {
    if (!client.isAuthenticated()) {
      Log.d(TAG, "Skipping now playing update, not logged in.");
      return;
    }

    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    if (!isConnected) {
      return;
    }
    client.updateNowPlaying(
        track,
        message -> {
          LastfmClient.Result result = (LastfmClient.Result) message.obj;
          int errorCode = result.errorCode();
          if (LastfmClient.isAuthenticationError(errorCode)) {
            notificationManager.notifyAuthError();
            ScroballApplication.getEventBus().post(AuthErrorEvent.create(errorCode));
          }
          return true;
        });
  }

  public void submit(PlaybackItem playbackItem) {
    // Set final value for amount played, in case it was playing up until now.
    playbackItem.updateAmountPlayed();

    // Generate one scrobble per played period.
    Track track = playbackItem.getTrack();

    if (!track.duration().isPresent()) {
      fetchTrackDurationAndSubmit(playbackItem);
      return;
    }

    long timestamp = playbackItem.getTimestamp();
    long duration = track.duration().get();
    long playTime = playbackItem.getAmountPlayed();

    if (playTime < 1) {
      return;
    }

    // Handle cases where player does not report duration *and* Last.fm does not report it either.
    if (duration == 0) {
      duration = playTime;
    }

    int playCount = (int) (playTime / duration);
    long scrobbleThreshold = Math.min(SCROBBLE_THRESHOLD, duration / 2);

    if (duration < MINIMUM_SCROBBLE_TIME) {
      return;
    }

    if (playTime % duration > scrobbleThreshold) {
      playCount++;
    }

    int newScrobbles = playCount - playbackItem.getPlaysScrobbled();
    for (int i = playbackItem.getPlaysScrobbled(); i < playCount; i++) {
      int itemTimestamp = (int) ((timestamp + i * duration) / 1000);

      Scrobble scrobble = Scrobble.builder().track(track).timestamp(itemTimestamp).build();

      pending.add(scrobble);
      scroballDB.writeScrobble(scrobble);
      playbackItem.addScrobble();
    }

    if (newScrobbles > 0) {
      Log.d(TAG, String.format("Queued %d scrobbles", playCount));
    }

    notificationManager.notifyScrobbled(track, newScrobbles);
    scrobblePending();
  }

  public void fetchTrackDurationAndSubmit(final PlaybackItem playbackItem) {
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    if (!isConnected || !client.isAuthenticated()) {
      Log.d(TAG, "Offline or unauthenticated, can't fetch track duration. Saving for later.");
      queuePendingPlaybackItem(playbackItem);
      return;
    }

    Track track = playbackItem.getTrack();
    client.getTrackInfo(
        track,
        message -> {
          if (message.obj == null) {
            Result result = Caller.getInstance().getLastResult();
            int errorCode = 1;

            if (result != null) {
              errorCode = result.getErrorCode();
            }
            if (errorCode == 6) {
              Log.d(TAG, "Track not found, cannot scrobble.");
              // TODO prompt user to scrobble anyway
            } else {
              if (LastfmClient.isTransientError(errorCode)) {
                Log.d(TAG, "Failed to fetch track duration, saving for later.");
                queuePendingPlaybackItem(playbackItem);
              }
              if (LastfmClient.isAuthenticationError(errorCode)) {
                notificationManager.notifyAuthError();
                ScroballApplication.getEventBus().post(AuthErrorEvent.create(errorCode));
              }
            }
            return true;
          }

          Track updatedTrack = (Track) message.obj;
          playbackItem.updateTrack(updatedTrack);
          Log.d(TAG, String.format("Track info updated: %s", playbackItem));

          submit(playbackItem);
          return true;
        });
  }

  public void scrobblePending() {
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    boolean tracksPending = !(pending.isEmpty() && pendingPlaybackItems.isEmpty());
    boolean backoff = lastScrobbleTime + nextScrobbleDelay > System.currentTimeMillis();

    if (isScrobbling || !tracksPending || !isConnected || !client.isAuthenticated() || backoff) {
      return;
    }

    List<PlaybackItem> playbackItems = new ArrayList<>(pendingPlaybackItems);
    pendingPlaybackItems.clear();
    scroballDB.clearPendingPlaybackItems();

    if (!playbackItems.isEmpty()) {
      Log.d(TAG, "Re-processing queued items with missing durations.");
    }

    for (PlaybackItem playbackItem : playbackItems) {
      fetchTrackDurationAndSubmit(playbackItem);
    }

    if (pending.isEmpty()) {
      return;
    }

    isScrobbling = true;
    final List<Scrobble> tracksToScrobble = new ArrayList<>(pending);

    while (tracksToScrobble.size() > MAX_SCROBBLES) {
      tracksToScrobble.remove(tracksToScrobble.size() - 1);
    }

    client.scrobbleTracks(
        tracksToScrobble,
        message -> {
          List<LastfmClient.Result> results = (List<LastfmClient.Result>) message.obj;
          boolean shouldBackoff = false;

          for (int i = 0; i < results.size(); i++) {
            LastfmClient.Result result = results.get(i);
            Scrobble scrobble = tracksToScrobble.get(i);

            if (result.isSuccessful()) {
              scrobble.status().setScrobbled(true);
              scroballDB.writeScrobble(scrobble);
              pending.remove(scrobble);
            } else {
              int errorCode = result.errorCode();
              if (!LastfmClient.isTransientError(errorCode)) {
                pending.remove(scrobble);
                shouldBackoff = true;
              }
              if (LastfmClient.isAuthenticationError(errorCode)) {
                notificationManager.notifyAuthError();
                ScroballApplication.getEventBus().post(AuthErrorEvent.create(errorCode));
              }
              scrobble.status().setErrorCode(errorCode);
              scroballDB.writeScrobble(scrobble);
            }
          }

          isScrobbling = false;
          lastScrobbleTime = System.currentTimeMillis();

          if (shouldBackoff) {
            // Back off starting at 1 second, up to an hour max.
            if (nextScrobbleDelay == 0) {
              nextScrobbleDelay = 1000;
            } else if (nextScrobbleDelay < 60 * 60 * 1000) {
              nextScrobbleDelay *= 4;
            }
          } else {
            nextScrobbleDelay = 0;

            // There may be more tracks waiting to scrobble. Keep going.
            scrobblePending();
          }
          return false;
        });
  }

  /**
   * Calculates the number of milliseconds of playback time remaining until the specified {@param
   * PlaybackItem} can be scrobbled, i.e. reaches 50% of track duration or SCROBBLE_THRESHOLD.
   *
   * @return The number of milliseconds remaining until the next scrobble for the current playback
   *     item can be submitted, or -1 if the track's duration is below MINIMUM_SCROBBLE_TIME.
   */
  public long getMillisecondsUntilScrobble(PlaybackItem playbackItem) {
    if (playbackItem == null) {
      return -1;
    }

    Optional<Long> optionalDuration = playbackItem.getTrack().duration();
    long duration = optionalDuration.or(0L);

    if (duration < MINIMUM_SCROBBLE_TIME) {
      if (optionalDuration.isPresent()) {
        Log.d(TAG, String.format("Not scheduling scrobble, track is too short (%d)", duration));
      } else {
        Log.d(TAG, "Not scheduling scrobble, track duration not known");
      }
      return -1;
    }

    long scrobbleThreshold = Math.min(duration / 2, SCROBBLE_THRESHOLD);
    long nextScrobbleAt = playbackItem.getPlaysScrobbled() * duration + scrobbleThreshold;

    return Math.max(0, nextScrobbleAt - playbackItem.getAmountPlayed());
  }

  private void queuePendingPlaybackItem(PlaybackItem playbackItem) {
    pendingPlaybackItems.add(playbackItem);
    scroballDB.writePendingPlaybackItem(playbackItem);
  }
}
