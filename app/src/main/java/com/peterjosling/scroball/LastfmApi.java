package com.peterjosling.scroball;

import java.util.List;

import com.softartdev.lastfm.Authenticator;
import com.softartdev.lastfm.Result;
import com.softartdev.lastfm.Session;
import com.softartdev.lastfm.Track;
import com.softartdev.lastfm.scrobble.ScrobbleData;
import com.softartdev.lastfm.scrobble.ScrobbleResult;

/**
 * Wrapper around the static {@link com.softartdev.lastfm} API methods to allow for the classes to be
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
   * @see Track#love(String, String, Session)
   */
  public Result love(String artistName, String trackName, Session session) {
    return Track.love(artistName, trackName, session);
  }

  /**
   * @see Authenticator#getSession(String, String, String)
   */
  public Session getSession(String token, String apiKey, String secret) {
    return Authenticator.getSession(token, apiKey, secret);
  }
}
