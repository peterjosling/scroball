package com.peterjosling.scroball.ui;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.peterjosling.scroball.NowPlayingChangeEvent;
import com.peterjosling.scroball.R;
import com.peterjosling.scroball.ScroballApplication;
import com.peterjosling.scroball.Track;

public class NowPlayingFragment extends Fragment {

  private static final String TAG = NowPlayingFragment.class.getName();

  private ViewGroup detailGroup;
  private ImageView artImageView;
  private TextView titleTextView;
  private TextView artistTextView;
  private TextView nothingPlayingTextView;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_now_playing, container, false);
    detailGroup = (ViewGroup) rootView.findViewById(R.id.now_playing_detail);
    artImageView = (ImageView) rootView.findViewById(R.id.now_playing_art);
    titleTextView = (TextView) rootView.findViewById(R.id.now_playing_title);
    artistTextView = (TextView) rootView.findViewById(R.id.now_playing_artist);
    nothingPlayingTextView = (TextView) rootView.findViewById(R.id.now_playing_nothing_playing);
    return rootView;
  }

  @Override
  public void onStart() {
    super.onStart();
    ScroballApplication.getEventBus().register(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    onNowPlayingChange(ScroballApplication.getLastNowPlayingChangeEvent());
  }

  @Override
  public void onStop() {
    ScroballApplication.getEventBus().unregister(this);
    super.onStop();
  }

  @Subscribe
  public void onNowPlayingChange(NowPlayingChangeEvent event) {
    Track track = event.track();
    Optional<Bitmap> art = track.art();

    if (track.isValid()) {
      String artistText = track.artist();
      if (track.album().isPresent()) {
        artistText = String.format("%s — %s", track.artist(), track.album().get());
      }

      titleTextView.setText(track.track());
      artistTextView.setText(artistText);

      if (art.isPresent()) {
        artImageView.setImageBitmap(art.get());
      } else {
        try {
          Drawable icon = getActivity().getPackageManager().getApplicationIcon(event.source());
          artImageView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
          Log.w(TAG, "Failed to read application icon for player", e);
        }
      }

      detailGroup.setVisibility(View.VISIBLE);
      nothingPlayingTextView.setVisibility(View.GONE);
    } else {
      detailGroup.setVisibility(View.GONE);
      nothingPlayingTextView.setVisibility(View.VISIBLE);
    }
  }
}
