package com.peterjosling.scroball;

import android.media.MediaMetadata;

import com.google.common.base.Optional;

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
    String track = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
    String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
    String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
    String albumArtist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
    long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

    if (track == null) {
      track = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);

      if (track  == null) {
        track = "";
      }
    }

    if (artist == null) {
      artist = "";

      MetadataExtractor extractor = new MetadataExtractor();
      Optional<Track> guess = extractor.guessTrack(track);

      if (guess.isPresent()) {
        Track guessedTrack = guess.get();
        artist = guessedTrack.artist();
        track = guessedTrack.track();
      }
    }

    ImmutableTrack.Builder builder = ImmutableTrack.builder();

    builder
        .track(track)
        .artist(artist)
        .album(Optional.fromNullable(album))
        .albumArtist(Optional.fromNullable(albumArtist));

    if (duration > 0) {
      builder.duration(duration);
    }

    return builder.build();
  }
}
