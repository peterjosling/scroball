package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AuthRequest {

  public abstract String username();
  public abstract String password();

  public static AuthRequest create(String username, String password) {
    return new AutoValue_AuthRequest(username, password);
  }
}
