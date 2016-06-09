package com.peterjosling.scroball;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateReceiver extends BroadcastReceiver {

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
        System.out.println("Network connected, scrobbling");
        scrobbler.scrobblePending();
      }
    }
  }
}
