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
   * @see Track#updateNowPlaying(ScrobbleData, Session)
   */
  public ScrobbleResult updateNowPlaying(ScrobbleData scrobbleData, Session session) {
    return Track.updateNowPlaying(scrobbleData, session);
  }

  /**
   * @see Authenticator#getSession(String, String, String)
   */
  public Session getSession(String token, String apiKey, String secret) {
    return Authenticator.getSession(token, apiKey, secret);
  }
}
