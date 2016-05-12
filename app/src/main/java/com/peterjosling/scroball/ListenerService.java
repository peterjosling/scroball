package com.peterjosling.scroball;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.service.notification.NotificationListenerService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;

public class ListenerService extends NotificationListenerService
    implements MediaSessionManager.OnActiveSessionsChangedListener {

  private List<MediaController> mediaControllers = new ArrayList<>();
  private Map<MediaController, MediaController.Callback> controllerCallbacks = new WeakHashMap<>();
  private PlaybackTracker playbackTracker;

  @Override
  public void onCreate() {
    ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

    ScrobbleLog scrobbleLog = new ScrobbleLog(new ScrobbleLogDbHelper(this));
    scrobbleLog.open();

    ScrobbleNotificationManager scrobbleNotificationManager =
        new ScrobbleNotificationManager(this);

    LastfmClient lastfmClient = new LastfmClient("tst", "");

    Scrobbler scrobbler = new Scrobbler(
        lastfmClient,
        scrobbleNotificationManager,
        scrobbleLog, connectivityManager);

    playbackTracker = new PlaybackTracker(
        scrobbleNotificationManager,
        scrobbleLog,
        connectivityManager,
        scrobbler);

    Log.d("Scroball", "NotificationListenerService started");

    MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext()
        .getSystemService(Context.MEDIA_SESSION_SERVICE);

    ComponentName componentName = new ComponentName(this, this.getClass());
    mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName);

    // Trigger change event with existing set of sessions.
    List<MediaController> initialSessions = mediaSessionManager.getActiveSessions(componentName);
    onActiveSessionsChanged(initialSessions);
  }

  @Override
  public void onActiveSessionsChanged(List<MediaController> activeMediaControllers) {
    Log.d("Scroball", "Active MediaSessions changed");

    Set<MediaController> existingControllers = new HashSet<>(mediaControllers);
    Set<MediaController> newControllers = new HashSet<>(activeMediaControllers);

    Set<MediaController> toRemove = Sets.difference(existingControllers, newControllers);
    Set<MediaController> toAdd = Sets.difference(newControllers, existingControllers);

    for (MediaController controller : toRemove) {
      if (controllerCallbacks.containsKey(controller)) {
        controller.unregisterCallback(controllerCallbacks.get(controller));
        playbackTracker.handleSessionTermination(controller.getPackageName());
      }
    }

    // TODO submit scrobbles for removed media sessions?

    for (final MediaController controller : toAdd) {
      if (!controller.getPackageName().equals("com.apple.android.music")) {
        // TODO remove
        System.out.println("Ignoring player");
        continue;
      }

      MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
          controllerPlaybackStateChanged(controller, state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
          controllerMetadataChanged(controller, metadata);
        }
      };

      controllerCallbacks.put(controller, callback);
      controller.registerCallback(callback);

      // Media may already be playing - update with current state.
      controllerPlaybackStateChanged(controller, controller.getPlaybackState());
      controllerMetadataChanged(controller, controller.getMetadata());
    }

    mediaControllers = activeMediaControllers;
  }

  private void controllerPlaybackStateChanged(MediaController controller, PlaybackState state) {
    playbackTracker.handlePlaybackStateChange(controller.getPackageName(), state);
  }

  private void controllerMetadataChanged(MediaController controller, MediaMetadata metadata) {
    playbackTracker.handleMetadataChange(controller.getPackageName(), metadata);
  }
}
