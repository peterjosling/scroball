package com.peterjosling.scroball;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.peterjosling.scroball.db.PendingPlaybackItemEntry;
import com.peterjosling.scroball.db.PendingPlaybackItemEntry_Table;
import com.peterjosling.scroball.db.ScrobbleLogEntry;
import com.peterjosling.scroball.db.ScrobbleLogEntry_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

/** FlowDB database to store scrobble history and pending scrobbles for the application. */
@Database(name = ScroballDB.NAME, version = ScroballDB.VERSION)
public class ScroballDB {

  static final String NAME = "ScroballDB";
  static final int VERSION = 2;

  private static final int MAX_ROWS = 1000;

  private EventBus eventBus = ScroballApplication.getEventBus();

  /** Returns a list of all pending and submitted {@link Scrobble}s. */
  public List<Scrobble> readScrobbles() {
    List<ScrobbleLogEntry> entries =
        SQLite.select()
            .from(ScrobbleLogEntry.class)
            .orderBy(ScrobbleLogEntry_Table.timestamp, false)
            .queryList();
    return scrobbleEntriesToScrobbles(entries);
  }

  /**
   * Writes a single scrobble to the database. If the scrobble already has already been written it
   * will be updated.
   */
  public void writeScrobble(Scrobble scrobble) {
    Track track = scrobble.track();
    ScrobbleStatus status = scrobble.status();
    ScrobbleLogEntry logEntry = new ScrobbleLogEntry();
    logEntry.timestamp = scrobble.timestamp();
    logEntry.artist = track.artist();
    logEntry.track = track.track();
    logEntry.status = scrobble.status().getErrorCode();

    if (track.album().isPresent()) {
      logEntry.album = track.album().get();
    }
    if (track.albumArtist().isPresent()) {
      logEntry.albumArtist = track.albumArtist().get();
    }
    if (status.getDbId() > -1) {
      logEntry.id = status.getDbId();
    }

    logEntry.save();
    scrobble.status().setDbId(logEntry.id);

    eventBus.post(ScroballDBUpdateEvent.create(scrobble));
  }

  /**
   * Writes a list of scrobbles to the database.
   *
   * @see #writeScrobble(Scrobble)
   */
  public void writeScrobbles(List<Scrobble> scrobbles) {
    for (Scrobble scrobble : scrobbles) {
      writeScrobble(scrobble);
    }
  }

  /**
   * Returns a list of all pending {@link PlaybackItem}s which have been written to the database.
   */
  public List<Scrobble> readPendingScrobbles() {
    List<ScrobbleLogEntry> entries =
        SQLite.select()
            .from(ScrobbleLogEntry.class)
            .where(ScrobbleLogEntry_Table.status.greaterThan(-1))
            .orderBy(ScrobbleLogEntry_Table.timestamp, false)
            .queryList();
    return scrobbleEntriesToScrobbles(entries);
  }

  /**
   * Writes a single {@link PlaybackItem} to the database. If the item has already been written it
   * will be updated.
   */
  public void writePendingPlaybackItem(PlaybackItem playbackItem) {
    Track track = playbackItem.getTrack();
    PendingPlaybackItemEntry entry = new PendingPlaybackItemEntry();
    entry.timestamp = playbackItem.getTimestamp();
    entry.artist = track.artist();
    entry.track = track.track();
    entry.amountPlayed = playbackItem.getAmountPlayed();

    if (track.album().isPresent()) {
      entry.album = track.album().get();
    }
    if (track.albumArtist().isPresent()) {
      entry.albumArtist = track.albumArtist().get();
    }
    if (playbackItem.getDbId() > -1) {
      entry.id = playbackItem.getDbId();
    }

    entry.save();
    playbackItem.setDbId(entry.id);
  }

  /** Returns a list of all {@link PlaybackItem}s which have been written to the database. */
  public List<PlaybackItem> readPendingPlaybackItems() {
    List<PendingPlaybackItemEntry> entries =
        SQLite.select()
            .from(PendingPlaybackItemEntry.class)
            .orderBy(PendingPlaybackItemEntry_Table.timestamp, true)
            .queryList();
    return pendingPlaybackEntriesToPlaybackItems(entries);
  }

  /**
   * Clears all {@link PlaybackItem}s from the database. This method should be called when pending
   * items have been queued for re-submission.
   */
  public void clearPendingPlaybackItems() {
    Delete.table(PendingPlaybackItemEntry.class);
  }

  /** Prunes old submitted {@link Scrobble}s from the database, limiting to {@code MAX_ROWS}. */
  public void prune() {
    long rowCount =
        SQLite.selectCountOf()
            .from(ScrobbleLogEntry.class)
            .where(ScrobbleLogEntry_Table.status.lessThan(0))
            .count();
    long toRemove = MAX_ROWS - rowCount;

    if (toRemove > 0) {
      SQLite.delete(ScrobbleLogEntry_Table.class)
          .where(ScrobbleLogEntry_Table.status.lessThan(0))
          .orderBy(ScrobbleLogEntry_Table.id, true)
          .limit((int) toRemove)
          .async()
          .execute();
    }
  }

  /** Clears all {@link Scrobble} and {@link PlaybackItem}s from the database. */
  public void clear() {
    Delete.tables(ScrobbleLogEntry.class, PendingPlaybackItemEntry.class);
  }

  private List<Scrobble> scrobbleEntriesToScrobbles(List<ScrobbleLogEntry> entries) {
    ImmutableList.Builder<Scrobble> builder = ImmutableList.builder();

    for (ScrobbleLogEntry entry : entries) {
      Track.Builder track = Track.builder().track(entry.track).artist(entry.artist);
      if (entry.albumArtist != null) {
        track.albumArtist(entry.albumArtist);
      }
      if (entry.album != null) {
        track.album(entry.album);
      }

      Scrobble scrobble =
          Scrobble.builder()
              .timestamp(entry.timestamp)
              .status(new ScrobbleStatus(entry.status, entry.id))
              .track(track.build())
              .build();

      builder.add(scrobble);
    }
    return builder.build();
  }

  private List<PlaybackItem> pendingPlaybackEntriesToPlaybackItems(
      List<PendingPlaybackItemEntry> entries) {
    ImmutableList.Builder<PlaybackItem> builder = ImmutableList.builder();

    for (PendingPlaybackItemEntry entry : entries) {
      Track.Builder track = Track.builder().track(entry.track).artist(entry.artist);
      if (entry.albumArtist != null) {
        track.albumArtist(entry.albumArtist);
      }
      if (entry.album != null) {
        track.album(entry.album);
      }
      builder.add(new PlaybackItem(track.build(), entry.timestamp, entry.amountPlayed, entry.id));
    }
    return builder.build();
  }
}
