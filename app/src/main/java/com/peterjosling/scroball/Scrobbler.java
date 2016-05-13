package com.peterjosling.scroball;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import de.umass.lastfm.scrobble.ScrobbleResult;

public class Scrobbler {

  private static final int SCROBBLE_THRESHOLD = 4 * 60 * 1000;
  private static final int MINIMUM_SCROBBLE_TIME = 30 * 1000;
  private static final int MAX_SCROBBLES = 50;

  private final LastfmClient client;
  private final ScrobbleNotificationManager notificationManager;
  private final ScrobbleLog scrobbleLog;
  private final ConnectivityManager connectivityManager;
  private final List<PlaybackItem> pendingPlaybackItems;
  private final List<Scrobble> pending;
  private boolean isScrobbling = false;

  public Scrobbler(
      LastfmClient client,
      ScrobbleNotificationManager notificationManager,
      ScrobbleLog scrobbleLog,
      ConnectivityManager connectivityManager) {
    this.client = client;
    this.notificationManager = notificationManager;
    this.scrobbleLog = scrobbleLog;
    this.connectivityManager = connectivityManager;
    // TODO read this from DB
    // TODO write unit test to ensure non-network plays get scrobbled with duration lookup.
    this.pendingPlaybackItems = new ArrayList<>();
    this.pending = scrobbleLog.readPending();
  }

  public void updateNowPlaying(Track track) {
    if (!client.isAuthenticated()) {
      System.out.println("Skipping now playing update, not logged in.");
      return;
    }

    System.out.println("!!!!!!!!!!! Updating now playing");
    System.out.println(track);
    client.updateNowPlaying(track.artist(), track.track());
  }

  public void submit(PlaybackItem playbackItem) {
    // Set final value for amount played, in case it was playing up until now.
    playbackItem = playbackItem.updateAmountPlayed();

    // Generate one scrobble per played period.
    Track track = playbackItem.track();

    if (!track.duration().isPresent()) {
      fetchTrackDurationAndSubmit(playbackItem);
      return;
    }

    System.out.println("Submitting: " + playbackItem);

    long timestamp = playbackItem.timestamp();
    long duration = track.duration().get();
    long playTime = playbackItem.amountPlayed();

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

    for (int i = 0; i < playCount; i++) {
      int itemTimestamp = (int) ((timestamp + i * duration) / 1000);

      // TODO set album title too
      Scrobble scrobble = ImmutableScrobble.builder()
          .track(track)
          .timestamp(itemTimestamp)
          .build();

      pending.add(scrobble);
      scrobbleLog.write(scrobble);
    }

    System.out.println("!!!!!!!!!!! Added new scrobbles: " + playCount);
    System.out.println(track);

    notificationManager.notifyScrobbled(track, playCount);
    scrobblePending();
  }

  public void fetchTrackDurationAndSubmit(final PlaybackItem playbackItem) {
    Track track = playbackItem.track();
    client.getTrackInfo(track.artist(), track.track(), new Handler.Callback() {
      @Override
      public boolean handleMessage(Message message) {
        PlaybackItem updatedPlaybackItem;

        // TODO error handling
        if (message.obj == null) {
          // TODO check error code here.
          // TODO persist this to DB.
          System.out.println("Failed to fetch track duration, saving for later.");
          pendingPlaybackItems.add(playbackItem);
          return true;
        }

        updatedPlaybackItem = ImmutablePlaybackItem.builder().from(playbackItem)
            .track((Track) message.obj)
            .build();
        System.out.println("Submitting updated track: " + updatedPlaybackItem);

        submit(updatedPlaybackItem);
        return true;
      }
    });
  }

  public void scrobblePending() {
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    boolean tracksPending = !(pending.isEmpty() && pendingPlaybackItems.isEmpty());

    // TODO listen for changes in connectivity and trigger this method then.
    if (isScrobbling || !tracksPending || !isConnected || !client.isAuthenticated()) {
      return;
    }

    List<PlaybackItem> playbackItems = new ArrayList<>(pendingPlaybackItems);
    pendingPlaybackItems.clear();

    if (!playbackItems.isEmpty()) {
      System.out.println("Re-processing queued items with missing durations.");
    }

    for (PlaybackItem playbackItem : playbackItems) {
      fetchTrackDurationAndSubmit(playbackItem);
    }

    isScrobbling = true;
    final List<Scrobble> tracksToScrobble = new ArrayList<>(pending);

    while (tracksToScrobble.size() > MAX_SCROBBLES) {
      tracksToScrobble.remove(tracksToScrobble.size() - 1);
    }

    client.scrobbleTracks(tracksToScrobble, new Handler.Callback() {
      @Override
      @SuppressWarnings(value = "unchecked")
      public boolean handleMessage(Message message) {
        List<ScrobbleResult> results = (List<ScrobbleResult>) message.obj;
        boolean didError = false;

        for (int i = 0; i < results.size(); i++) {
          ScrobbleResult result = results.get(i);
          Scrobble scrobble = tracksToScrobble.get(i);

          if (result.isSuccessful()) {
            scrobble.status().setScrobbled(true);
            scrobbleLog.write(scrobble);
            pending.remove(scrobble);
          } else {
            // TODO set error code.
            scrobble.status().setErrorCode(1);
            scrobbleLog.write(scrobble);
            didError = true;
          }
        }

        isScrobbling = false;

        // TODO need to wait if there was an error/rate limiting
        if (!didError) {
          scrobblePending();
        }

        return false;
      }
    });
  }
}
