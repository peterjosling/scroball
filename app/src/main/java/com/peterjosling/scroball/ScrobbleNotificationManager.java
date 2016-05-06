package com.peterjosling.scroball;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.util.Locale;

public class ScrobbleNotificationManager {

  private static final int NOW_PLAYING_ID = 0;

  private final Context context;
  private final NotificationManager notificationManager;
  private int scrobbleNotificationId = 1;

  public ScrobbleNotificationManager(Context context) {
    this.context = context;
    this.notificationManager = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public void updateNowPlaying(Track track) {
    Notification notification = new Notification.Builder(context)
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("Now playing")
        .setContentText(String.format("%s — %s", track.artist(), track.track()))
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .build();

    notificationManager.notify(NOW_PLAYING_ID, notification);
  }

  public void removeNowPlaying() {
    notificationManager.cancel(NOW_PLAYING_ID);
  }

  public void notifyScrobbled(Track track, int count) {
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
        .build();

    notificationManager.notify(scrobbleNotificationId++, notification);
  }
}
