package com.peterjosling.scroball;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

@AutoValue
public abstract class AuthResult {

  public abstract Optional<String> sessionKey();
  public abstract Optional<Integer> httpErrorCode();
  public abstract Optional<Integer> errorCode();
  public abstract Optional<String> error();

  public static Builder builder() {
    return new AutoValue_AuthResult.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder sessionKey(String sessionKey);
    abstract Builder httpErrorCode(int httpErrorCode);
    abstract Builder errorCode(int errorCode);
    abstract Builder error(String error);
    abstract AuthResult build();
  }
}
