package com.peterjosling.scroball;

import java.util.List;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

/**
 * Wrapper around the static {@link de.umass.lastfm} API methods to allow for the classes to be
 * mocked out in tests.
 */
public class LastfmApi {

  /**
   * @see Track#getInfo(String, String, String)
   */
  public Track getTrackInfo(String artist, String trackOrMbid, String apiKey) {
    return Track.getInfo(artist, trackOrMbid, apiKey);
  }

  /**
   * @see Track#scrobble(List, Session)
   */
  public List<ScrobbleResult> scrobble(List<ScrobbleData> scrobbleData, Session session) {
    return Track.scrobble(scrobbleData, session);
  }

  /**
   * @see Track#updateNowPlaying(String, String, Session)
   */
  public ScrobbleResult updateNowPlaying(String artistName, String trackName, Session session) {
    return Track.updateNowPlaying(artistName, trackName, session);
  }

  /**
   * @see Authenticator#getSession(String, String, String)
   */
  public Session getSession(String token, String apiKey, String secret) {
    return Authenticator.getSession(token, apiKey, secret);
  }
}
