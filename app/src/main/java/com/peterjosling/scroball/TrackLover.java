package com.peterjosling.scroball;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.common.eventbus.EventBus;
import com.peterjosling.scroball.db.LovedTracksEntry;
import com.peterjosling.scroball.db.ScroballDB;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Handles the submission of track love actions to the Last.fm API. Writes loves to
 * {@link ScroballDB} and queues submissions for later if it cannot be completed immediately.
 *
 * Triggering the submission of queued entries is handled by {@param Scrobbler}.
 */
public class TrackLover {

  private final LastfmClient lastfmClient;
  private final ScroballDB scroballDB;
  private final ConnectivityManager connectivityManager;
  private final EventBus eventBus = ScroballApplication.getEventBus();
  private final Queue<LovedTracksEntry> pending = new LinkedList<>();
  private boolean isSubmitting = false;

  public TrackLover(
      LastfmClient lastfmClient, ScroballDB scroballDB, ConnectivityManager connectivityManager) {
    this.lastfmClient = lastfmClient;
    this.scroballDB = scroballDB;
    this.connectivityManager = connectivityManager;
    pending.addAll(scroballDB.readPendingLoves());
  }

  /**
   * Submits a single {@param track} to be loved. If the network is offline or the submission
   * fails for a transient reason, it will be queued and retried later.
   */
  public synchronized void loveTrack(Track track) {
    if (scroballDB.isLoved(track)) {
      return;
    }

    LovedTracksEntry entry = scroballDB.writeLove(track, 0);
    pending.add(entry);
    lovePending();
    eventBus.post(TrackLovedEvent.create());
  }

  /**
   * Submits all pending tracks to be loved, one at a time. Tracks are queued for submission
   * later if the network is offline, or the submission fails for some transient reason.
   */
  public synchronized void lovePending() {
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    if (isSubmitting || !isConnected || pending.isEmpty()) {
      return;
    }

    isSubmitting = true;

    LovedTracksEntry entry = pending.remove();
    Track track = Track.builder().track(entry.track).artist(entry.artist).build();
    lastfmClient.loveTrack(
        track,
        message -> {
          LastfmClient.Result result = (LastfmClient.Result) message.obj;
          entry.status = result.errorCode();
          entry.save();

          if (!result.isSuccessful()) {
            pending.add(entry);
          }
          isSubmitting = false;
          lovePending();
          return true;
        });
  }
}
