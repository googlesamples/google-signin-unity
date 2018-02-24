/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.googlesignin;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.PendingResult;
import java.util.Locale;

/** Helper class containing the request for information. */
public class TokenRequest {
  private TokenPendingResult pendingResponse;
  private boolean useGamesConfig;
  private boolean doAuthCode;
  private boolean doEmail;
  private boolean doIdToken;
  private String webClientId;
  private boolean forceRefresh;
  private boolean hidePopups;
  private String accountName;
  private String[] scopes;
  private long handle;

  /**
   * Constructs a token request.
   *
   * @param useGamesConfig - true if the GAMES_CONFIG should be used when signing-in.
   * @param webClientId - the web client id of the backend server associated with this application.
   * @param requestAuthCode - true if a server auth code is needed. This also requires the web
   *     client id to be set.
   * @param forceRefreshToken - true to force a refresh token when using the server auth code.
   * @param requestEmail - true if email address of the user is requested.
   * @param requestIdToken - true if an id token for the user is requested.
   * @param hideUiPopups - true if the popups during sign-in from the Games API should be hidden.
   *     This only has affect if useGamesConfig is true.
   * @param defaultAccountName - the account name to attempt to default to when signing in.
   * @param additionalScopes - additional API scopes to request when authenticating.
   * @param requestHandle - the handle to this request, created by the native C++ code, this is used
   *     to correlate the response with the request.
   */
  public TokenRequest(
      boolean useGamesConfig,
      String webClientId,
      boolean requestAuthCode,
      boolean forceRefreshToken,
      boolean requestEmail,
      boolean requestIdToken,
      boolean hideUiPopups,
      String defaultAccountName,
      String[] additionalScopes,
      long requestHandle) {
    pendingResponse = new TokenPendingResult(requestHandle);
    this.useGamesConfig = useGamesConfig;
    this.webClientId = webClientId;
    this.doAuthCode = requestAuthCode;
    this.forceRefresh = forceRefreshToken;
    this.doEmail = requestEmail;
    this.doIdToken = requestIdToken;
    this.hidePopups = hideUiPopups;
    this.accountName = defaultAccountName;
    this.handle = requestHandle;
    if (additionalScopes != null && additionalScopes.length > 0) {
      scopes = new String[additionalScopes.length];
      System.arraycopy(additionalScopes, 0, scopes, 0, additionalScopes.length);
    } else {
      scopes = null;
    }
  }

  /**
   * Returns the pending response object for this request.
   *
   * @return the pending response.
   */
  public PendingResult<TokenResult> getPendingResponse() {
    return pendingResponse;
  }

  /**
   * Sets the result of the reuquest.
   *
   * @param code - the status code of the request.
   * @param account - the GoogleSignInAccount if successful.
   */
  public void setResult(int code, GoogleSignInAccount account) {
    pendingResponse.setResult(account, code);
    pendingResponse.setStatus(code);
  }

  /** Cancels the request and notifies the pending response. */
  public void cancel() {
    pendingResponse.cancel();
  }

  @Override
  public String toString() {
    return String.format(
        Locale.getDefault(),
        "%s(a:%b:e:%b:i:%b)",
        Integer.toHexString(hashCode()),
        doAuthCode,
        doEmail,
        doIdToken);
  }

  public String getWebClientId() {
    return webClientId == null ? "" : webClientId;
  }

  public boolean getForceRefresh() {
    return forceRefresh;
  }

  public boolean isValid() {
    if (webClientId == null || webClientId.isEmpty()) {
      if (doAuthCode) {
        GoogleSignInHelper.logError(
            "Invalid configuration, auth code" + " requires web " + "client id");
        return false;
      } else if (doIdToken) {
        GoogleSignInHelper.logError("Invalid configuration, id token requires web " + "client id");
        return false;
      }
    }
    return true;
  }

  public long getHandle() {
    return handle;
  }

  public boolean getUseGamesConfig() {
    return useGamesConfig;
  }

  public boolean getDoAuthCode() {
    return doAuthCode;
  }

  public boolean getDoEmail() {
    return doEmail;
  }

  public boolean getDoIdToken() {
    return doIdToken;
  }

  public String[] getScopes() {
    return scopes;
  }

  public String getAccountName() {
    return accountName;
  }

  public boolean getHidePopups() {
    return hidePopups;
  }
}
