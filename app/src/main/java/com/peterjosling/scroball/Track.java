package com.peterjosling.scroball;

import android.media.MediaMetadata;

import com.google.common.base.Optional;
import com.peterjosling.scroball.transforms.TitleExtractor;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Track {

  public abstract String track();
  public abstract String artist();
  public abstract Optional<String> album();
  public abstract Optional<String> albumArtist();
  @Value.Auxiliary
  public abstract Optional<Long> duration();

  public boolean isValid() {
    return !track().equals("") && !artist().equals("");
  }

  public static Track fromMediaMetadata(MediaMetadata metadata) {
    String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
    String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
    String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
    String albumArtist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
    long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

    if (title == null) {
      title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);

      if (title  == null) {
        title = "";
      }
    }

    ImmutableTrack.Builder builder = ImmutableTrack.builder()
        .track(title)
        .artist(artist)
        .album(Optional.fromNullable(album))
        .albumArtist(Optional.fromNullable(albumArtist));

    if (duration > 0) {
      builder.duration(duration);
    }

    if (artist == null) {
      new TitleExtractor().transform(builder.build());
    }
    return builder.build();
  }
}
