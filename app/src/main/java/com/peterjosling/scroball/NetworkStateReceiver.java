package com.peterjosling.scroball;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkStateReceiver extends BroadcastReceiver {

  private static final String TAG = NetworkStateReceiver.class.getName();

  private final Scrobbler scrobbler;

  public NetworkStateReceiver(Scrobbler scrobbler) {
    this.scrobbler = scrobbler;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getExtras() != null) {
      NetworkInfo networkInfo =
          (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);

      if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
        Log.i(TAG, "Network connected, scrobbling");
        scrobbler.scrobblePending();
      }
    }
  }
}
