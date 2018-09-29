package com.peterjosling.scroball;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class CastListener extends MediaRouter.Callback
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private final Context context;
  private final MediaRouter mediaRouter;
  private final MediaRouteSelector mediaRouteSelector;

  public CastListener(Context context) {
    this.context = context;

    mediaRouter = MediaRouter.getInstance(context);
    mediaRouteSelector =
        new MediaRouteSelector.Builder()
            .addControlCategory("com.google.android.gms.cast.CATEGORY_CAST")
            .build();

    mediaRouter.addCallback(mediaRouteSelector, this, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
  }

  @Override
  public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
    System.out.println("ROUTE ADDED: " + route);
    CastDevice device = CastDevice.getFromBundle(route.getExtras());
    Cast.CastOptions.Builder apiOptionsBuilder =
        Cast.CastOptions.builder(device, new Cast.Listener() {
          @Override
          public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
            super.onApplicationMetadataChanged(applicationMetadata);
          }
        });
    GoogleApiClient apiClient =
        new GoogleApiClient.Builder(context)
            .addApi(Cast.API, apiOptionsBuilder.build())
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    apiClient.connect();
  }

  @Override
  public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
    System.out.println("ROUTE CHANGED: " + route);
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    System.out.println("ROUTE CONNECTED");
  }

  @Override
  public void onConnectionSuspended(int i) {}

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
}
