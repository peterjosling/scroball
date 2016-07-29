package com.peterjosling.scroball;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScrobbleNotificationManager {

  private static final int NOW_PLAYING_ID = 0;
  private static final int SCROBBLE_ID = 1;
  private static final String NOTIFICATION_DISMISS_ACTION = "scrobble_notification_dismissed";

  private final Context context;
  private final SharedPreferences sharedPreferences;
  private final NotificationManager notificationManager;
  private final List<Track> tracks = new ArrayList<>();
  private final Map<Track, Integer> playCounts = new HashMap<>();

  public ScrobbleNotificationManager(Context context, SharedPreferences sharedPreferences) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
    this.notificationManager = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);

    context.registerReceiver(
        new NotificationDismissedReceiver(), new IntentFilter(NOTIFICATION_DISMISS_ACTION));
  }

  public void updateNowPlaying(Track track) {
    if (!sharedPreferences.getBoolean("notifications_now_playing", true)) {
      return;
    }

    Notification notification = new Notification.Builder(context)
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("Now playing")
        .setContentText(String.format("%s — %s", track.artist(), track.track()))
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .setColor(Color.argb(255, 242, 72, 63))
        .build();

    notificationManager.notify(NOW_PLAYING_ID, notification);
  }

  public void removeNowPlaying() {
    notificationManager.cancel(NOW_PLAYING_ID);
  }

  public void notifyScrobbled(Track track, int count) {
    if (!sharedPreferences.getBoolean("notifications_scrobble", true)) {
      return;
    }

    if (count < 1) {
      return;
    }

    if (playCounts.containsKey(track)) {
      count += playCounts.get(track);
    } else {
      tracks.add(track);
    }

    playCounts.put(track, count);

    List<String> descriptions = new ArrayList<>();

    for (Track t : tracks) {
      String plays = "";
      int c = playCounts.get(t);

      if (c > 1) {
        plays = String.format(Locale.getDefault(), "%d plays: ", c);
      }

      descriptions.add(String.format("%s%s — %s", plays, t.artist(), t.track()));
    }

    String text = String.format(Locale.getDefault(), "%d tracks", tracks.size());
    String title = "Tracks scrobbled";

    if (tracks.size() == 1) {
      text = descriptions.get(0);
      title = "Track scrobbled";
    }

    Joiner joiner = Joiner.on("\n");
    Intent intent = new Intent(NOTIFICATION_DISMISS_ACTION);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context, SCROBBLE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder notificationBuilder = new Notification.Builder(context)
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle(title)
        .setContentText(text)
        .setCategory(Notification.CATEGORY_STATUS)
        .setColor(Color.argb(255, 139, 195, 74))
        .setDeleteIntent(pendingIntent);

    if (tracks.size() > 1) {
      notificationBuilder
          .setStyle(new Notification.BigTextStyle().bigText(joiner.join(descriptions)));
    }

    notificationManager.notify(SCROBBLE_ID, notificationBuilder.build());
  }

  public class NotificationDismissedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      tracks.clear();
      playCounts.clear();
    }
  }
}
