package com.peterjosling.scroball;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.ConnectivityManager;
import android.service.notification.NotificationListenerService;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.peterjosling.scroball.db.ScroballDB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ListenerService extends NotificationListenerService
    implements MediaSessionManager.OnActiveSessionsChangedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = ListenerService.class.getName();

  private List<MediaController> mediaControllers = new ArrayList<>();
  private Map<MediaController, MediaController.Callback> controllerCallbacks = new WeakHashMap<>();
  private PlaybackTracker playbackTracker;
  private SharedPreferences sharedPreferences;

  @Override
  public void onCreate() {
    ScroballApplication application = (ScroballApplication) getApplication();
    sharedPreferences = application.getSharedPreferences();

    ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    ScroballDB scroballDB = application.getScroballDB();
    ScrobbleNotificationManager scrobbleNotificationManager =
        new ScrobbleNotificationManager(this, sharedPreferences, scroballDB);
    LastfmClient lastfmClient = application.getLastfmClient();
    TrackLover trackLover = new TrackLover(lastfmClient, scroballDB, connectivityManager);
    Scrobbler scrobbler =
        new Scrobbler(
            lastfmClient, scrobbleNotificationManager, scroballDB, connectivityManager, trackLover);

    playbackTracker = new PlaybackTracker(scrobbleNotificationManager, scrobbler);

    Log.d(TAG, "NotificationListenerService started");

    MediaSessionManager mediaSessionManager =
        (MediaSessionManager)
            getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);

    ComponentName componentName = new ComponentName(this, this.getClass());
    mediaSessionManager.addOnActiveSessionsChangedListener(this, componentName);

    NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver(scrobbler);
    IntentFilter filter = new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
    this.registerReceiver(networkStateReceiver, filter);

    // Trigger change event with existing set of sessions.
    List<MediaController> initialSessions = mediaSessionManager.getActiveSessions(componentName);
    onActiveSessionsChanged(initialSessions);

    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
  }

  public static boolean isNotificationAccessEnabled(Context context) {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.getPackageName());
  }

  @Override
  public void onActiveSessionsChanged(List<MediaController> activeMediaControllers) {
    Log.d(TAG, "Active MediaSessions changed");

    Set<MediaController> existingControllers =
        ImmutableSet.copyOf(Iterables.filter(mediaControllers, controllerCallbacks::containsKey));
    Set<MediaController> newControllers = new HashSet<>(activeMediaControllers);

    Set<MediaController> toRemove = Sets.difference(existingControllers, newControllers);
    Set<MediaController> toAdd = Sets.difference(newControllers, existingControllers);

    for (MediaController controller : toRemove) {
      controller.unregisterCallback(controllerCallbacks.get(controller));
      playbackTracker.handleSessionTermination(controller.getPackageName());
      controllerCallbacks.remove(controller);
    }

    for (final MediaController controller : toAdd) {
      String packageName = controller.getPackageName();
      String prefKey = "player." + packageName;

      if (!sharedPreferences.contains(prefKey)) {
        boolean defaultVal = sharedPreferences.getBoolean("scrobble_new_players", true);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(prefKey, defaultVal);
        editor.apply();
      }

      if (!sharedPreferences.getBoolean(prefKey, true)) {
        Log.d(TAG, String.format("Ignoring player %s", packageName));
        continue;
      }

      Log.d(TAG, String.format("Listening for events from %s", packageName));

      MediaController.Callback callback =
          new MediaController.Callback() {
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

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.startsWith("player.")) {
      final String packageName = key.substring(7);

      if (sharedPreferences.getBoolean(key, true)) {
        Log.d(TAG, "Player enabled, re-registering callbacks");
        onActiveSessionsChanged(mediaControllers);
      } else {
        Log.d(TAG, "Player disabled, stopping any current tracking");
        final Optional<MediaController> optionalController =
            Iterables.tryFind(
                mediaControllers, input -> input.getPackageName().equals(packageName));

        if (optionalController.isPresent()
            && controllerCallbacks.containsKey(optionalController.get())) {
          MediaController controller = optionalController.get();
          controller.unregisterCallback(controllerCallbacks.get(controller));
          playbackTracker.handleSessionTermination(controller.getPackageName());
          controllerCallbacks.remove(controller);
        }
      }
    }
  }

  private void controllerPlaybackStateChanged(MediaController controller, PlaybackState state) {
    playbackTracker.handlePlaybackStateChange(controller.getPackageName(), state);
  }

  private void controllerMetadataChanged(MediaController controller, MediaMetadata metadata) {
    playbackTracker.handleMetadataChange(controller.getPackageName(), metadata);
  }
}
