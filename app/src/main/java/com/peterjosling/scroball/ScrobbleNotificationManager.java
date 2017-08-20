package com.peterjosling.scroball;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.peterjosling.scroball.ui.MainActivity;
import com.peterjosling.scroball.ui.SplashScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScrobbleNotificationManager {

  private static final int NOW_PLAYING_ID = 0;
  private static final int SCROBBLE_ID = 1;
  private static final int AUTH_ERROR_ID = 2;

  private static final String CHANNEL_ID_SCROBBLE = "scrobble";
  private static final String CHANNEL_ID_ERROR = "error";
  private static final String CHANNEL_ID_NOW_PLAYING = "now_playing";

  private static final String NOTIFICATION_DISMISS_ACTION = "scrobble_notification_dismissed";

  private final Context context;
  private final SharedPreferences sharedPreferences;
  private final NotificationManager notificationManager;
  private final List<Track> tracks = new ArrayList<>();
  private final Map<Track, Integer> playCounts = new HashMap<>();

  public ScrobbleNotificationManager(Context context, SharedPreferences sharedPreferences) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
    this.notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    context.registerReceiver(
        new NotificationDismissedReceiver(), new IntentFilter(NOTIFICATION_DISMISS_ACTION));

    // Create notification channels, where available.
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      NotificationChannel scrobbleChannel =
          new NotificationChannel(
              CHANNEL_ID_SCROBBLE,
              context.getString(R.string.notification_channel_name_scrobble),
              NotificationManager.IMPORTANCE_DEFAULT);

      NotificationChannel errorChannel =
          new NotificationChannel(
              CHANNEL_ID_ERROR,
              context.getString(R.string.notification_channel_name_error),
              NotificationManager.IMPORTANCE_HIGH);
      errorChannel.enableLights(true);
      errorChannel.setLightColor(Color.RED);
      errorChannel.enableVibration(true);
      errorChannel.setSound(
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
          new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());

      NotificationChannel nowPlayingChannel =
          new NotificationChannel(
              CHANNEL_ID_NOW_PLAYING,
              context.getString(R.string.notification_channel_name_now_playing),
              NotificationManager.IMPORTANCE_DEFAULT);

      notificationManager.createNotificationChannels(
          ImmutableList.of(scrobbleChannel, errorChannel, nowPlayingChannel));
    }
  }

  public void updateNowPlaying(Track track) {
    if (!sharedPreferences.getBoolean("notifications_now_playing", true)) {
      return;
    }

    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

    Notification.Builder notification =
        new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Now playing")
            .setContentText(String.format("%s — %s", track.artist(), track.track()))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(Color.argb(255, 242, 72, 63))
            .setContentIntent(pendingIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notification.setChannelId(CHANNEL_ID_NOW_PLAYING);
    }

    notificationManager.notify(NOW_PLAYING_ID, notification.build());
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
    PendingIntent dismissIntent =
        PendingIntent.getBroadcast(
            context,
            SCROBBLE_ID,
            new Intent(NOTIFICATION_DISMISS_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT);

    Intent clickIntent = new Intent(context, MainActivity.class);
    clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    clickIntent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_SCROBBLE_HISTORY);
    PendingIntent clickPendingIntent =
        PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    Notification.Builder notificationBuilder =
        new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(Color.argb(255, 139, 195, 74))
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(dismissIntent)
            .setNumber(tracks.size())
            .setAutoCancel(true);

    if (tracks.size() > 1) {
      notificationBuilder.setStyle(
          new Notification.BigTextStyle().bigText(joiner.join(descriptions)));
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationBuilder.setChannelId(CHANNEL_ID_SCROBBLE);
    }

    notificationManager.notify(SCROBBLE_ID, notificationBuilder.build());
  }

  public void notifyAuthError() {
    int color = Color.argb(255, 242, 72, 63);
    PendingIntent contentIntent =
        PendingIntent.getActivity(
            context, 0, new Intent(context, SplashScreen.class), PendingIntent.FLAG_CANCEL_CURRENT);

    Notification.Builder notification =
        new Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.authentication_error_title))
            .setContentText(context.getString(R.string.authentication_error_content))
            .setCategory(Notification.CATEGORY_ERROR)
            .setColor(color)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notification.setChannelId(CHANNEL_ID_NOW_PLAYING);
    } else {
      notification
          .setPriority(Notification.PRIORITY_HIGH)
          .setLights(color, 500, 500)
          .setVibrate(new long[] {0, 200, 100, 500});
    }

    notificationManager.notify(AUTH_ERROR_ID, notification.build());
  }

  public class NotificationDismissedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      tracks.clear();
      playCounts.clear();
    }
  }
}
