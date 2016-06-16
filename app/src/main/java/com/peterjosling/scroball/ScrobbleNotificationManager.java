package com.peterjosling.scroball;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.Locale;

public class ScrobbleNotificationManager {

  private static final int NOW_PLAYING_ID = 0;

  private final Context context;
  private final SharedPreferences sharedPreferences;
  private final NotificationManager notificationManager;
  private int scrobbleNotificationId = 1;

  public ScrobbleNotificationManager(Context context, SharedPreferences sharedPreferences) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
    this.notificationManager = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);
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

    String plays = "";

    if (count < 1) {
      return;
    }

    if (count > 1) {
      plays = String.format(Locale.getDefault(), "%d plays: ", count);
    }

    Notification notification = new Notification.Builder(context)
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("Track scrobbled")
        .setContentText(String.format("%s%s — %s", plays, track.artist(), track.track()))
        .setCategory(Notification.CATEGORY_STATUS)
        .setColor(Color.argb(255, 139, 195, 74))
        .build();

    notificationManager.notify(scrobbleNotificationId++, notification);
  }
}
